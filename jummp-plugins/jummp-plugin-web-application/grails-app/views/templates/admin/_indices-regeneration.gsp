<li class="accordion-item" data-accordion-item>
    <!-- Accordion tab title -->
    <a href="${createLink(controller: 'search', action: 'regen')}" class="accordion-title">Indices
    Regeneration</a>
    <!-- Accordion tab content: it would start in the open state due to using the `is-active` state class. -->
    <div class="accordion-content" data-tab-content>
        <p>Re-index models</p>
        <a href="${createLink(controller: 'search', action: 'regen')}">Access service</a>
    </div>
</li>
