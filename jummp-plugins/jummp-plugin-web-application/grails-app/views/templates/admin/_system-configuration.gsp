<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 12/06/2020
  Time: 16:42
--%>
<li class="accordion-item" data-accordion-item>
    <!-- Accordion tab title -->
    <a href="${createLink(controller: 'configuration')}" class="accordion-title">System Configuration</a>
    <!-- Accordion tab content: it would start in the open state due to using the `is-active` state class. -->
    <div class="accordion-content" data-tab-content>
        <p>Manage System Configuration</p>
        <a href="${createLink(controller: 'configuration')}">Access service</a>
    </div>
</li>
