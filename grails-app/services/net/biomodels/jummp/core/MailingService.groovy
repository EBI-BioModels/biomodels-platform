/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/

package net.biomodels.jummp.core

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Pattern

/**
 * Unified email dispatch service.
 *
 * Routes outgoing mail through the configured provider in priority order:
 *   1. Brevo HTTP API  (jummp.security.mailer.brevoApiKey set)
 *   2. smtp2go HTTP API (jummp.security.mailer.apiKey set)
 *   3. JavaMail / SMTP  (fallback — requires working SMTP port)
 *
 * HTTP API providers use HTTPS (port 443) and are not affected by SMTP port
 * blocks common in restricted networks and EC2 environments.
 *
 * Usage:
 *   mailingService.send([
 *       to      : "user@example.com",
 *       subject : "Hello",
 *       html    : "<p>Body</p>",   // or text: "plain body"
 *       from    : "sender@example.com",  // optional, defaults to system sender
 *       bcc     : ["admin@example.com"], // optional
 *       replyTo : "reply@example.com"    // optional
 *   ])
 */
class MailingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailingService.class)
    static transactional = false

    def grailsApplication
    def mailService

    private static final Pattern VALID_EMAIL = ~/^[^@\s]+@[^@\s]+\.[^@\s]+$/
    private static final Pattern NAME_EMAIL = ~/^.+?\s*<([^>]+)>$/

    private static String extractEmail(String addr) {
        if (!addr) return addr
        def m = addr =~ NAME_EMAIL
        return m ? (m[0][1] as String).trim() : addr.trim()
    }

    void send(Map params) {
        String toAddr      = extractEmail(params.to as String)
        if (!toAddr || !(toAddr ==~ VALID_EMAIL)) {
            LOGGER.warn("Skipping email — invalid recipient address: '${params.to}'")
            return
        }
        String subjectStr  = params.subject as String
        String htmlBody    = params.html as String
        String textBody    = params.text as String
        String fromAddr    = (params.from ?: grailsApplication.config.jummp.security.registration.email.sender) as String
        List   bccList     = params.bcc ? [params.bcc].flatten() as List<String> : null
        String replyToAddr = params.replyTo ? extractEmail(params.replyTo as String) : null
        if (replyToAddr && !(replyToAddr ==~ VALID_EMAIL)) {
            LOGGER.warn("Dropping invalid replyTo address: '${params.replyTo}'")
            replyToAddr = null
        }

        def brevoKey   = grailsApplication.config.jummp.security.mailer.brevoApiKey
        def smtp2goKey = grailsApplication.config.jummp.security.mailer.apiKey

        if (brevoKey && !(brevoKey instanceof ConfigObject)) {
            sendViaBrevoApi(toAddr, htmlBody ?: textBody, subjectStr, fromAddr,
                    brevoKey as String, htmlBody != null, bccList, replyToAddr)
        } else if (smtp2goKey && !(smtp2goKey instanceof ConfigObject)) {
            sendViaSmtp2goApi(toAddr, htmlBody ?: textBody, subjectStr, fromAddr,
                    smtp2goKey as String, htmlBody != null, bccList)
        } else {
            mailService.sendMail {
                to toAddr
                from fromAddr
                subject subjectStr
                if (htmlBody) html htmlBody
                else text textBody
                if (bccList) bcc bccList
                if (replyToAddr) replyTo replyToAddr
            }
        }
    }

    private void sendViaBrevoApi(
        String toEmail, String body, String subject, String sender,
        String apiKey, boolean isHtml, List bcc, String replyTo) {
        final String API_URL = "https://api.brevo.com/v3/smtp/email"
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("api-key", apiKey)
        conn.setDoOutput(true)
        conn.setConnectTimeout(10000)
        conn.setReadTimeout(30000)

        def match = sender =~ /^(.+?)\s*<([^>]+)>$/
        Map senderMap = match ? [name: match[0][1].trim(), email: match[0][2].trim()] : [email: sender.trim()]

        Map payload = [
            sender : senderMap,
            to     : [[email: toEmail]],
            subject: subject
        ]
        if (isHtml) payload.htmlContent = body
        else payload.textContent = body
        if (bcc) {
            payload.bcc = bcc.collect { [email: it as String] } as Serializable
        }
        if (replyTo) payload.replyTo = [email: replyTo]

        conn.outputStream.withWriter("UTF-8") { it.write(new JsonBuilder(payload).toString()) }
        int status = conn.responseCode
        if (status != 201) {
            String errorBody = conn.errorStream?.text ?: "(no error body)"
            LOGGER.error("An error happened when using Brevo API: ${status}: $errorBody")
            throw new RuntimeException("Brevo API returned HTTP $status: $errorBody")
        }
    }

    private void sendViaSmtp2goApi(
        String toEmail, String body, String subject, String sender,
        String apiKey, boolean isHtml, List bcc) {
        final String API_URL = "https://api.smtp2go.com/v3/email/send"
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setDoOutput(true)
        conn.setConnectTimeout(10000)
        conn.setReadTimeout(30000)

        Map payload = [
            api_key: apiKey,
            to     : [toEmail],
            sender : sender,
            subject: subject
        ]
        if (isHtml) payload.html_body = body
        else payload.text_body = body
        if (bcc) {
            payload.bcc = bcc as Serializable
        }

        conn.outputStream.withWriter("UTF-8") { it.write(new JsonBuilder(payload).toString()) }
        int status = conn.responseCode
        if (status != 200) {
            throw new RuntimeException("smtp2go API returned HTTP $status")
        }
        def json = new JsonSlurper().parseText(conn.inputStream.text)
        if (!json?.data || (json.data.succeeded as int) < 1) {
            LOGGER.error("An error happened when using smtp2go API: ${json?.data?.failures}")
            throw new RuntimeException("smtp2go API: email not sent — ${json?.data?.failures}")
        }
    }
}
