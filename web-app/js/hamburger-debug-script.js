/**
 * Hamburger Menu Debugger
 * Inject this script into your page to track when and why the hamburger menu appears
 *
 * Usage:
 * 1. Open your website
 * 2. Open browser console (F12)
 * 3. Copy and paste this entire script
 * 4. Resize the window and watch the console output
 */

(function () {
    'use strict';

    console.log('%c🔍 Hamburger Menu Debugger Started', 'background: #4CAF50; color: white; padding: 10px; font-size: 16px; font-weight: bold;');

    // Configuration - adjust these selectors to match your site
    const SELECTORS = {
        hamburger: [
            '.hamburger',
            '.hamburger-menu',
            '.navbar-toggler',
            '.mobile-menu-toggle',
            '.menu-toggle',
            '[class*="hamburger"]',
            '[class*="toggle"]',
            'button[aria-label*="menu" i]',
            'button[aria-expanded]'
        ],
        desktopMenu: [
            '.desktop-menu',
            '.navbar-nav',
            '.main-nav',
            '.primary-nav',
            '.navigation',
            'nav ul',
            '[class*="desktop-menu"]',
            '[class*="main-nav"]'
        ]
    };

    // Find elements
    function findElement(selectors) {
        for (let selector of selectors) {
            const elements = document.querySelectorAll(selector);
            if (elements.length > 0) {
                return Array.from(elements);
            }
        }
        return [];
    }

    const hamburgerElements = findElement(SELECTORS.hamburger);
    const desktopMenuElements = findElement(SELECTORS.desktopMenu);

    console.log('📍 Found Elements:');
    console.log('  Hamburger candidates:', hamburgerElements.length);
    hamburgerElements.forEach((el, i) => {
        console.log(`    ${i + 1}. ${el.tagName}.${el.className}`, el);
    });

    console.log('  Desktop menu candidates:', desktopMenuElements.length);
    desktopMenuElements.forEach((el, i) => {
        console.log(`    ${i + 1}. ${el.tagName}.${el.className}`, el);
    });

    // Get computed style information
    function getElementDebugInfo(element) {
        const computed = window.getComputedStyle(element);
        return {
            display: computed.display,
            visibility: computed.visibility,
            opacity: computed.opacity,
            width: computed.width,
            height: computed.height,
            position: computed.position,
            zIndex: computed.zIndex,
            transform: computed.transform
        };
    }

    // Get all active media queries
    function getActiveMediaQueries() {
        const active = [];
        const sheets = Array.from(document.styleSheets);

        sheets.forEach(sheet => {
            try {
                const rules = Array.from(sheet.cssRules || sheet.rules || []);
                rules.forEach(rule => {
                    if (rule instanceof CSSMediaRule) {
                        if (window.matchMedia(rule.conditionText || rule.media.mediaText).matches) {
                            active.push({
                                query: rule.conditionText || rule.media.mediaText,
                                sheet: sheet.href || 'inline'
                            });
                        }
                    }
                });
            } catch (e) {
                // Cross-origin stylesheets will throw errors
                console.log('  ⚠️ Cannot access stylesheet:', sheet.href);
            }
        });

        return active;
    }

    // Print current state
    function printDebugInfo(trigger = 'Manual') {
        console.log('\n' + '='.repeat(80));
        console.log(`%c📊 DEBUG SNAPSHOT (${trigger})`, 'background: #2196F3; color: white; padding: 5px; font-weight: bold;');
        console.log('='.repeat(80));

        // Viewport info
        console.log('\n🖥️  VIEWPORT INFORMATION:');
        console.log(`  Window Inner Width:    ${window.innerWidth}px`);
        console.log(`  Window Inner Height:   ${window.innerHeight}px`);
        console.log(`  Document Client Width: ${document.documentElement.clientWidth}px`);
        console.log(`  Screen Width:          ${screen.width}px`);
        console.log(`  Device Pixel Ratio:    ${window.devicePixelRatio}`);
        console.log(`  Orientation:           ${window.innerWidth > window.innerHeight ? 'Landscape' : 'Portrait'}`);

        // Active media queries
        console.log('\n📱 ACTIVE MEDIA QUERIES:');
        const activeQueries = getActiveMediaQueries();
        if (activeQueries.length === 0) {
            console.log('  No media queries detected or unable to access stylesheets');
        } else {
            activeQueries.forEach(q => {
                console.log(`  ✓ ${q.query}`);
                console.log(`    Source: ${q.sheet}`);
            });
        }

        // Hamburger menu status
        console.log('\n🍔 HAMBURGER MENU STATUS:');
        if (hamburgerElements.length === 0) {
            console.log('  ❌ No hamburger menu elements found!');
        } else {
            hamburgerElements.forEach((el, i) => {
                const info = getElementDebugInfo(el);
                const isVisible = info.display !== 'none' &&
                    info.visibility !== 'hidden' &&
                    parseFloat(info.opacity) > 0;

                console.log(`  Element ${i + 1}: ${el.tagName}.${el.className || '(no class)'}`);
                console.log(`    Visible: %c${isVisible ? 'YES' : 'NO'}`,
                    isVisible ? 'color: red; font-weight: bold' : 'color: green');
                console.log(`    Display: ${info.display}`);
                console.log(`    Visibility: ${info.visibility}`);
                console.log(`    Opacity: ${info.opacity}`);
                console.log(`    Dimensions: ${info.width} x ${info.height}`);
            });
        }

        // Desktop menu status
        console.log('\n🖥️  DESKTOP MENU STATUS:');
        if (desktopMenuElements.length === 0) {
            console.log('  ❌ No desktop menu elements found!');
        } else {
            desktopMenuElements.forEach((el, i) => {
                const info = getElementDebugInfo(el);
                const isVisible = info.display !== 'none' &&
                    info.visibility !== 'hidden' &&
                    parseFloat(info.opacity) > 0;

                console.log(`  Element ${i + 1}: ${el.tagName}.${el.className || '(no class)'}`);
                console.log(`    Visible: %c${isVisible ? 'YES' : 'NO'}`,
                    isVisible ? 'color: green' : 'color: red; font-weight: bold');
                console.log(`    Display: ${info.display}`);
                console.log(`    Visibility: ${info.visibility}`);
                console.log(`    Opacity: ${info.opacity}`);
            });
        }

        // Check for Bootstrap
        console.log('\n🅱️  FRAMEWORK DETECTION:');
        if (typeof window.bootstrap !== 'undefined') {
            console.log('  ✓ Bootstrap detected');
        }
        if (typeof window.jQuery !== 'undefined') {
            console.log('  ✓ jQuery detected (v' + window.jQuery.fn.jquery + ')');
        }

        console.log('\n' + '='.repeat(80) + '\n');
    }

    // Monitor visibility changes using MutationObserver
    if (hamburgerElements.length > 0) {
        console.log('\n👁️  Setting up visibility monitoring...');

        hamburgerElements.forEach((el, i) => {
            const observer = new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    if (mutation.type === 'attributes' &&
                        (mutation.attributeName === 'style' ||
                            mutation.attributeName === 'class')) {

                        const info = getElementDebugInfo(el);
                        const isVisible = info.display !== 'none' &&
                            info.visibility !== 'hidden' &&
                            parseFloat(info.opacity) > 0;

                        console.log(`%c🔄 Hamburger ${i + 1} visibility changed!`,
                            'background: #FF9800; color: white; padding: 5px;');
                        console.log(`   Now: ${isVisible ? 'VISIBLE' : 'HIDDEN'}`);
                        console.log(`   Width: ${window.innerWidth}px`);
                        console.log(`   Display: ${info.display}`);

                        if (isVisible) {
                            printDebugInfo('Visibility Change');
                        }
                    }
                });
            });

            observer.observe(el, {
                attributes: true,
                attributeFilter: ['style', 'class']
            });
        });
    }

    // Monitor resize events
    let resizeTimeout;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(() => {
            console.log(`%c📏 Window resized to ${window.innerWidth}px`,
                'background: #9C27B0; color: white; padding: 5px;');
            printDebugInfo('Resize');
        }, 500);
    });

    // Initially snapshot
    setTimeout(() => printDebugInfo('Initial Load'), 1000);

    // Add helper functions to the window
    window.hamburgerDebug = {
        snapshot: () => printDebugInfo('Manual Call'),
        findHamburger: () => hamburgerElements,
        findDesktopMenu: () => desktopMenuElements,
        testWidth: (width) => {
            console.log(`%c🧪 Testing width: ${width}px`, 'background: #00BCD4; color: white; padding: 5px;');
            window.resizeTo(width, window.innerHeight);
        },
        getMediaQueries: () => {
            const queries = getActiveMediaQueries();
            console.table(queries);
            return queries;
        },
        help: () => {
            console.log('%cAvailable Commands:', 'font-weight: bold; font-size: 14px;');
            console.log('  hamburgerDebug.snapshot()       - Print current state');
            console.log('  hamburgerDebug.findHamburger()  - Get hamburger elements');
            console.log('  hamburgerDebug.findDesktopMenu() - Get desktop menu elements');
            console.log('  hamburgerDebug.getMediaQueries() - List active media queries');
            console.log('  hamburgerDebug.help()           - Show this help');
        }
    };

    console.log('\n✅ Debugger ready! Type "hamburgerDebug.help()" for commands');
    console.log('💡 Resize the window to see real-time debugging\n');

})();
