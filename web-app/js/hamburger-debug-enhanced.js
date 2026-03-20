/**
 * Enhanced Hamburger Menu Debugger - CORS-Safe Version
 * Works with external stylesheets by testing common breakpoints
 * 
 * Usage:
 * 1. Open your website
 * 2. Open browser console (F12)
 * 3. Copy and paste this entire script
 * 4. Resize the window and watch the console output
 */

(function() {
    'use strict';
    
    console.log('%c🔍 Enhanced Hamburger Menu Debugger Started (CORS-Safe)', 'background: #4CAF50; color: white; padding: 10px; font-size: 16px; font-weight: bold;');
    
    // Common breakpoints to test
    const COMMON_BREAKPOINTS = [
        { name: 'XS (Mobile)', value: 480 },
        { name: 'SM (Mobile)', value: 576 },
        { name: 'MD (Tablet)', value: 768 },
        { name: 'LG (Small Desktop)', value: 992 },
        { name: 'XL (Desktop)', value: 1024 },
        { name: 'XL (Desktop)', value: 1200 },
        { name: 'XXL (Large Desktop)', value: 1400 }
    ];
    
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
            'button[aria-expanded]',
            '.nav-toggle',
            '#nav-toggle',
            '[id*="toggle"]',
            '[class*="mobile-nav"]'
        ],
        desktopMenu: [
            '.desktop-menu',
            '.navbar-nav',
            '.main-nav',
            '.primary-nav',
            '.navigation',
            'nav ul',
            '[class*="desktop-menu"]',
            '[class*="main-nav"]',
            '.navbar-collapse',
            '#navbar-collapse'
        ]
    };
    
    // Find elements
    function findElement(selectors) {
        for (let selector of selectors) {
            try {
                const elements = document.querySelectorAll(selector);
                if (elements.length > 0) {
                    return Array.from(elements);
                }
            } catch (e) {
                // Invalid selector, continue
            }
        }
        return [];
    }
    
    const hamburgerElements = findElement(SELECTORS.hamburger);
    const desktopMenuElements = findElement(SELECTORS.desktopMenu);
    
    console.log('📍 Found Elements:');
    console.log('  Hamburger candidates:', hamburgerElements.length);
    hamburgerElements.forEach((el, i) => {
        console.log(`    ${i + 1}. ${el.tagName}.${el.className || el.id || '(no class/id)'}`, el);
    });
    
    console.log('  Desktop menu candidates:', desktopMenuElements.length);
    desktopMenuElements.forEach((el, i) => {
        console.log(`    ${i + 1}. ${el.tagName}.${el.className || el.id || '(no class/id)'}`, el);
    });
    
    // Get computed style information
    function getElementDebugInfo(element) {
        const computed = window.getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        return {
            display: computed.display,
            visibility: computed.visibility,
            opacity: computed.opacity,
            width: computed.width,
            height: computed.height,
            position: computed.position,
            zIndex: computed.zIndex,
            transform: computed.transform,
            overflow: computed.overflow,
            pointerEvents: computed.pointerEvents,
            // Actual dimensions
            actualWidth: rect.width,
            actualHeight: rect.height,
            top: rect.top,
            left: rect.left
        };
    }
    
    // Test breakpoints using matchMedia
    function testBreakpoints() {
        const results = [];
        
        COMMON_BREAKPOINTS.forEach(bp => {
            const queries = [
                `(max-width: ${bp.value}px)`,
                `(max-width: ${bp.value - 0.02}px)`,
                `(max-width: ${bp.value - 1}px)`,
                `(min-width: ${bp.value}px)`,
                `(min-width: ${bp.value + 1}px)`
            ];
            
            queries.forEach(query => {
                if (window.matchMedia(query).matches) {
                    results.push({
                        breakpoint: bp.name,
                        value: bp.value,
                        query: query,
                        matches: true
                    });
                }
            });
        });
        
        return results;
    }
    
    // Get all stylesheets info (without accessing cross-origin content)
    function getStylesheetInfo() {
        const sheets = [];
        Array.from(document.styleSheets).forEach((sheet, index) => {
            try {
                const info = {
                    index: index,
                    href: sheet.href || 'inline styles',
                    disabled: sheet.disabled,
                    accessible: false,
                    ruleCount: 0
                };
                
                // Try to access rules (will fail for CORS)
                try {
                    info.ruleCount = sheet.cssRules ? sheet.cssRules.length : 0;
                    info.accessible = true;
                } catch (e) {
                    info.accessible = false;
                    info.corsBlocked = true;
                }
                
                sheets.push(info);
            } catch (e) {
                // Skip problematic sheets
            }
        });
        return sheets;
    }
    
    // Reverse engineer the breakpoint by testing visibility at different widths
    function findBreakpointByElement(element) {
        const currentWidth = window.innerWidth;
        const testResults = [];
        
        // Test each common breakpoint
        COMMON_BREAKPOINTS.forEach(bp => {
            const testQueries = [
                `(max-width: ${bp.value}px)`,
                `(max-width: ${bp.value - 0.02}px)`,
                `(min-width: ${bp.value}px)`,
                `(min-width: ${bp.value + 1}px)`
            ];
            
            testQueries.forEach(query => {
                if (window.matchMedia(query).matches) {
                    testResults.push({
                        breakpoint: bp.value,
                        query: query,
                        currentWidth: currentWidth
                    });
                }
            });
        });
        
        return testResults;
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
        
        // Stylesheet info
        console.log('\n📄 STYLESHEET INFORMATION:');
        const sheets = getStylesheetInfo();
        sheets.forEach(sheet => {
            console.log(`  [${sheet.index}] ${sheet.href}`);
            console.log(`      Accessible: ${sheet.accessible ? '✓ YES' : '✗ NO (CORS blocked)'}`);
            if (sheet.accessible) {
                console.log(`      Rules: ${sheet.ruleCount}`);
            }
        });
        
        // Active breakpoints
        console.log('\n📱 ACTIVE BREAKPOINT TESTS:');
        const activeBreakpoints = testBreakpoints();
        if (activeBreakpoints.length === 0) {
            console.log('  No standard breakpoints matched');
        } else {
            console.log(`  Current width ${window.innerWidth}px matches:`);
            activeBreakpoints.forEach(bp => {
                console.log(`    ✓ ${bp.query} (${bp.breakpoint})`);
            });
        }
        
        // Find likely breakpoint
        console.log('\n🎯 LIKELY BREAKPOINT ANALYSIS:');
        const width = window.innerWidth;
        const sortedBP = [...COMMON_BREAKPOINTS].sort((a, b) => b.value - a.value);
        
        for (let bp of sortedBP) {
            if (width <= bp.value) {
                console.log(`%c  → Your screen (${width}px) is <= ${bp.value}px (${bp.name})`, 
                           'color: red; font-weight: bold;');
                console.log(`     Likely trigger: @media (max-width: ${bp.value}px) or similar`);
            } else {
                console.log(`  ✓ Your screen (${width}px) is > ${bp.value}px (${bp.name})`);
            }
        }
        
        // Hamburger menu status
        console.log('\n🍔 HAMBURGER MENU STATUS:');
        if (hamburgerElements.length === 0) {
            console.log('  ❌ No hamburger menu elements found!');
            console.log('  💡 Try adding more selectors or inspect the HTML manually');
        } else {
            hamburgerElements.forEach((el, i) => {
                const info = getElementDebugInfo(el);
                const isVisible = info.display !== 'none' && 
                                 info.visibility !== 'hidden' && 
                                 parseFloat(info.opacity) > 0 &&
                                 info.actualWidth > 0 &&
                                 info.actualHeight > 0;
                
                console.log(`  Element ${i + 1}: ${el.tagName}.${el.className || el.id || '(no identifier)'}`);
                console.log(`    Visible: %c${isVisible ? 'YES ⚠️' : 'NO'}`, 
                           isVisible ? 'color: red; font-weight: bold;' : 'color: green');
                console.log(`    Display: ${info.display}`);
                console.log(`    Visibility: ${info.visibility}`);
                console.log(`    Opacity: ${info.opacity}`);
                console.log(`    Computed Dimensions: ${info.width} x ${info.height}`);
                console.log(`    Actual Dimensions: ${info.actualWidth.toFixed(2)}px x ${info.actualHeight.toFixed(2)}px`);
                console.log(`    Position: ${info.position} (top: ${info.top.toFixed(2)}px, left: ${info.left.toFixed(2)}px)`);
                
                if (isVisible) {
                    console.log(`%c    ⚠️ HAMBURGER IS VISIBLE AT ${window.innerWidth}px!`, 
                               'background: yellow; color: red; font-weight: bold; padding: 3px;');
                }
                
                // Log full computed styles for debugging
                console.log('    Full element:', el);
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
                
                console.log(`  Element ${i + 1}: ${el.tagName}.${el.className || el.id || '(no identifier)'}`);
                console.log(`    Visible: %c${isVisible ? 'YES' : 'NO ⚠️'}`, 
                           isVisible ? 'color: green' : 'color: red; font-weight: bold;');
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
        if (document.querySelector('[class*="ebi-"]') || document.querySelector('[href*="ebi"]')) {
            console.log('  ✓ EBI Framework detected (from EMBL-EBI)');
        }
        
        // Recommendations
        console.log('\n💡 RECOMMENDATIONS:');
        if (hamburgerElements.length > 0) {
            const info = getElementDebugInfo(hamburgerElements[0]);
            const isVisible = info.display !== 'none' && 
                             info.visibility !== 'hidden' && 
                             parseFloat(info.opacity) > 0 &&
                             info.actualWidth > 0;
            
            if (isVisible && window.innerWidth > 768) {
                console.log(`%c  ⚠️ PROBLEM DETECTED!`, 'background: red; color: white; font-weight: bold; padding: 5px;');
                console.log(`  The hamburger menu is visible at ${window.innerWidth}px`);
                console.log(`  This is wider than the typical mobile breakpoint (768px)`);
                console.log('');
                console.log('  Likely causes:');
                
                // Find which breakpoint range we're in
                for (let i = 0; i < COMMON_BREAKPOINTS.length; i++) {
                    const bp = COMMON_BREAKPOINTS[i];
                    if (window.innerWidth <= bp.value) {
                        console.log(`  → Your CSS probably has: @media (max-width: ${bp.value}px)`);
                        console.log(`     Location: Check ${sheets.find(s => !s.accessible)?.href || 'external stylesheets'}`);
                        console.log('');
                        console.log('  To fix:');
                        console.log(`  1. Find this media query in your CSS files`);
                        console.log(`  2. Change ${bp.value}px to 768px (or lower)`);
                        console.log('  3. Or add this override to your custom CSS:');
                        console.log('');
                        console.log(`     @media (min-width: 769px) and (max-width: ${bp.value}px) {`);
                        console.log('       .navbar-toggler, .hamburger { display: none !important; }');
                        console.log('       .navbar-collapse, .desktop-menu { display: flex !important; }');
                        console.log('     }');
                        break;
                    }
                }
            } else if (isVisible && window.innerWidth <= 768) {
                console.log('  ✓ Hamburger is correctly shown on mobile/tablet');
            } else {
                console.log('  ✓ Hamburger is correctly hidden on desktop');
            }
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
                                         parseFloat(info.opacity) > 0 &&
                                         info.actualWidth > 0;
                        
                        console.log(`%c🔄 Hamburger ${i + 1} changed!`, 
                                   'background: #FF9800; color: white; padding: 5px;');
                        console.log(`   Now: ${isVisible ? 'VISIBLE' : 'HIDDEN'}`);
                        console.log(`   Width: ${window.innerWidth}px`);
                        console.log(`   Display: ${info.display}`);
                        
                        if (isVisible) {
                            setTimeout(() => printDebugInfo('Visibility Change'), 100);
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
    
    // Monitor resize events with detailed logging
    let resizeTimeout;
    let lastWidth = window.innerWidth;
    
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimeout);
        
        const newWidth = window.innerWidth;
        const direction = newWidth > lastWidth ? '→ WIDER' : '← NARROWER';
        
        console.log(`%c📏 Resizing ${direction}: ${lastWidth}px → ${newWidth}px`, 
                   'background: #9C27B0; color: white; padding: 3px;');
        
        lastWidth = newWidth;
        
        resizeTimeout = setTimeout(() => {
            printDebugInfo('Resize Complete');
        }, 500);
    });
    
    // Add helper functions to window
    window.hamburgerDebug = {
        snapshot: () => printDebugInfo('Manual Call'),
        findHamburger: () => {
            console.log('Found hamburger elements:', hamburgerElements);
            return hamburgerElements;
        },
        findDesktopMenu: () => {
            console.log('Found desktop menu elements:', desktopMenuElements);
            return desktopMenuElements;
        },
        testBreakpoint: (width) => {
            console.log(`\n🧪 TESTING WIDTH: ${width}px`);
            console.log('Breakpoints that would trigger at this width:');
            COMMON_BREAKPOINTS.forEach(bp => {
                if (width <= bp.value) {
                    console.log(`  ✓ ${bp.name} (max-width: ${bp.value}px) - WOULD TRIGGER`);
                } else {
                    console.log(`    ${bp.name} (max-width: ${bp.value}px) - would not trigger`);
                }
            });
        },
        inspectElement: (element) => {
            if (!element) {
                console.log('Usage: hamburgerDebug.inspectElement(document.querySelector(".your-selector"))');
                return;
            }
            console.log('\n🔍 ELEMENT INSPECTION:');
            console.log('Element:', element);
            console.log('Debug info:', getElementDebugInfo(element));
        },
        help: () => {
            console.log('%cAvailable Commands:', 'font-weight: bold; font-size: 14px;');
            console.log('  hamburgerDebug.snapshot()           - Print current state');
            console.log('  hamburgerDebug.findHamburger()      - Get hamburger elements');
            console.log('  hamburgerDebug.findDesktopMenu()    - Get desktop menu elements');
            console.log('  hamburgerDebug.testBreakpoint(992)  - Test what triggers at width');
            console.log('  hamburgerDebug.inspectElement(el)   - Inspect any element');
            console.log('  hamburgerDebug.help()               - Show this help');
        }
    };
    
    // Initial snapshot after page loads
    setTimeout(() => {
        printDebugInfo('Initial Load');
        console.log('\n✅ Debugger ready! Type "hamburgerDebug.help()" for commands');
        console.log('💡 Resize the window to see real-time debugging');
        console.log('🔧 If no hamburger found, inspect your HTML and add the selector\n');
    }, 1000);
    
})();
