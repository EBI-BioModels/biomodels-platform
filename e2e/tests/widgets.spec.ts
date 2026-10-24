
import { expect, test, isMobile, MODEL_ID, skipUnlessModelExists, waitForFoundation, watchProblems } from './support';

test.describe('modals (Foundation reveal)', () => {
  const MODALS = [
    ['search tips', 'searchTipsBox'],
    ['domain switcher help', 'domainSwitcherExplanationBox'],
  ];

  for (const [name, id] of MODALS) {
    test(`${name} modal opens and closes`, async ({ page }) => {
      const problems = watchProblems(page);
      await page.goto('');
      await waitForFoundation(page);
      const modal = page.locator(`#${id}`);
      await expect(modal).toBeHidden();
      await page.locator(`[data-open="${id}"]`).first().click();
      await expect(modal).toBeVisible();
      await modal.locator('.close-button').click();
      await expect(modal).toBeHidden();
      problems.assertClean();
    });
  }

  test('a modal closes with the Escape key', async ({ page }) => {
    await page.goto('');
    await waitForFoundation(page);
    const modal = page.locator('#searchTipsBox');
    await page.locator('[data-open="searchTipsBox"]').first().click();
    await expect(modal).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(modal).toBeHidden();
  });

  test('feedback modal opens from the menu', async ({ page }) => {
    await page.goto('');
    await waitForFoundation(page);
    if (isMobile()) await page.locator('#biomodels-hamburger').click();
    await page.locator('[data-open="rate_review_form"]').first().click();
    await expect(page.locator('#rate_review_form')).toBeVisible();
  });
});

test('FAQ accordion items expand and collapse and report their state', async ({ page }) => {
  const problems = watchProblems(page);
  await page.goto('faq');
  await waitForFoundation(page);
  const items = page.locator('#faq-accordion > .accordion-item');
  const item = items.nth(1);
  const title = item.locator('.accordion-title');
  const content = item.locator('.accordion-content');

  // Every title says what its panel is doing, including the items that start open.
  const count = await items.count();
  for (let i = 0; i < count; i++) {
    const open = ((await items.nth(i).getAttribute('class')) || '').includes('is-active');
    await expect(items.nth(i).locator('.accordion-title'), `item ${i}`).toHaveAttribute('aria-expanded', String(open));
  }

  const opening = ((await item.getAttribute('class')) || '').includes('is-active');
  const settle = async (open: boolean) => {
    if (open) {
      await expect(item).toHaveClass(/is-active/);
      await expect(content).toBeVisible();
    } else {
      await expect(item).not.toHaveClass(/is-active/);
      await expect(content).toBeHidden();
    }
    await expect(title).toHaveAttribute('aria-expanded', String(open));
  };
  await settle(opening);
  await title.click();
  await settle(!opening);
  await title.click();
  await settle(opening);
  problems.assertClean();
});

test('home carousel moves to the slide that is picked', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  const slides = page.locator('.orbit-slide');
  await expect(slides).toHaveCount(3);
  const bullets = page.locator('.orbit-bullets button');
  const settled = page.locator('.orbit-slide.mui-enter-active, .orbit-slide.mui-leave-active');
  await bullets.nth(2).click();
  await expect(slides.nth(2)).toHaveClass(/is-active/);
  await expect(settled).toHaveCount(0);
  await bullets.nth(0).click();
  await expect(slides.nth(0)).toHaveClass(/is-active/);
  await expect(page.locator('.orbit-slide.is-active')).toHaveCount(1);
});

test('model page tabs and file preview modal work', async ({ page }) => {
  const problems = watchProblems(page);
  await skipUnlessModelExists(page);
  await page.goto(MODEL_ID);
  await waitForFoundation(page);
  const overview = page.locator('#btn-Overview');
  const files = page.locator('#btn-Files');
  const history = page.locator('#btn-History');
  await expect(overview).toHaveClass(/w3-red/);

  await files.click();
  await expect(files).toHaveClass(/w3-red/);
  await expect(overview).not.toHaveClass(/w3-red/);

  const preview = page.locator('[data-open="filePreviewBox"]:visible').first();
  if (await preview.count()) {
    await preview.click();
    await expect(page.locator('#filePreviewBox')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.locator('#filePreviewBox')).toBeHidden();
  }

  await history.click();
  await expect(history).toHaveClass(/w3-red/);
  await expect(files).not.toHaveClass(/w3-red/);
  problems.assertClean();
});
