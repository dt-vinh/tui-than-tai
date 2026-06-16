# Vision OCR and category pipeline

## What the app should do

Users take or import a photo. The app suggests expense fields, then the user confirms before saving.

Required behavior:

- Receipt, bill, transfer screenshot, money note, price tag: extract the best amount candidate and prefill amount.
- Object photo without a visible price, such as a watch: do not invent amount; suggest a category such as Shopping.
- Medicine, first-aid or bandage photo: suggest Health.
- Food or drink photo: suggest Food and drink.
- Offline mode: category suggestions and basic OCR should still work; online AI can improve difficult receipts.

## Chosen stack

1. ML Kit Text Recognition v2
   - Used for offline OCR on receipts, bills, money notes, price tags and screenshots.
   - Current dependency: `com.google.mlkit:text-recognition:16.0.1`.
   - Good fit because it runs on device and supports the "no network, still capture manually" flow.

2. ML Kit Image Labeling
   - Used for offline object-photo category suggestions when OCR has no clear money text.
   - Current dependency: `com.google.mlkit:image-labeling:17.0.9`.
   - The app maps image labels to app categories through `ObjectCategoryClassifier`.

3. Gemini Vision API
   - Used only when an API key and network are available.
   - Best for difficult receipts, bank apps, e-commerce screenshots and mixed-language images.
   - Must return structured JSON and the app still asks the user to review before saving.

4. ML Kit Object Detection
   - Not primary for this app right now.
   - The base model only gives coarse classes such as fashion goods, food, home goods, places and plants, so image labeling is better for category hints.
   - Object detection can be added later if we need bounding boxes or auto-cropping.

## Current Android implementation

- `ExpenseAnalyzer` tries Gemini first when configured.
- If Gemini is unavailable, it uses ML Kit OCR and the local total resolver.
- If OCR is blank, low-signal, `other`, or cannot find an amount, it runs ML Kit Image Labeling.
- `ObjectCategoryClassifier` maps labels:
  - watch, fashion accessory, electronics, bag, clothing -> `shopping`
  - medicine, pill, bandage, first aid -> `health`
  - bread, food, fast food, beverage -> `food_and_drink`
  - vehicle, car, bus, train, motorcycle -> `transport`
- `LuckyWalletViewModel` maps those category IDs to visible app categories.

## UX rule

The app may prefill amount only from OCR or online vision extraction. For object-only images, the amount stays empty/0 and `needsReview=true`; the category is only a suggestion.

## Sources

- ML Kit Text Recognition v2 Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- ML Kit Image Labeling Android: https://developers.google.com/ml-kit/vision/image-labeling/android
- ML Kit Object Detection Android: https://developers.google.com/ml-kit/vision/object-detection/android
- Gemini image understanding: https://ai.google.dev/gemini-api/docs/image-understanding
- Gemini structured output: https://ai.google.dev/gemini-api/docs/structured-output
