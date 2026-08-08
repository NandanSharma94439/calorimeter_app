require('dotenv').config();

const admin = require("firebase-admin");
const express = require('express');
const axios = require('axios');
const cors = require('cors');
const multer = require('multer');
const rateLimit = require('express-rate-limit');

// ── Firebase Init ────────────────────────────────────────────────────────────
const serviceAccount = JSON.parse(process.env.FIREBASE_KEY);

if (!admin.apps.length) {
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}

const db = admin.firestore();
const app = express();

// ── Middleware ───────────────────────────────────────────────────────────────
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Multer — in-memory storage for image uploads (no disk I/O)
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max
  fileFilter: (req, file, cb) => {
    const mime = (file.mimetype || '').toLowerCase();
    const name = (file.originalname || '').toLowerCase();
    const isImage = mime.startsWith('image/') || mime === 'application/octet-stream' || /\.(jpg|jpeg|png|webp)$/i.test(name) || !mime;
    if (isImage) cb(null, true);
    else cb(null, true); // Allow upload so buffer is processed safely
  },
});

// Rate limiter — prevent abuse on AI endpoint (costly calls)
const aiLimiter = rateLimit({
  windowMs: 60 * 1000,       // 1 minute
  max: 10,                    // 10 requests per minute per IP
  message: { error: 'Too many requests. Please wait a moment.' },
});

const generalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  message: { error: 'Too many requests.' },
});

app.use(generalLimiter);

// ── Constants ────────────────────────────────────────────────────────────────
const COLLECTION = 'foods';

const pieceWeights = {
  banana: 118, egg: 50, apple: 182,
  roti: 40, bread: 25, orange: 131,
  potato: 150, tomato: 123,
};

// ── API Key Auth ─────────────────────────────────────────────────────────────
app.use((req, res, next) => {
  if (req.headers['x-api-key'] !== process.env.SECRET_API_KEY) {
    return res.status(403).json({ error: 'Unauthorized' });
  }
  next();
});

// ── Helpers ──────────────────────────────────────────────────────────────────
function calculateNutrition(per100g, grams) {
  return Number(((per100g * grams) / 100).toFixed(1));
}

function getNutrientVal(nutriments, keys) {
  if (!nutriments) return 0;
  for (const k of keys) {
    if (nutriments[k] !== undefined && nutriments[k] !== null && !isNaN(Number(nutriments[k])) && Number(nutriments[k]) > 0) {
      return Number(nutriments[k]);
    }
  }
  return 0;
}

function extractGrams(quantityText) {
  if (!quantityText) return 100;
  const q = String(quantityText).toLowerCase().trim();

  // Multi pack: "2 x 25g" or "2x25g" or "2 x 25 g"
  const multiPack = q.match(/(\d+)\s*x\s*(\d+(\.\d+)?)\s*g/);
  if (multiPack) return Number(multiPack[1]) * Number(multiPack[2]);

  // Kg: "1kg", "0.5 kg"
  const kg = q.match(/(\d+(\.\d+)?)\s*kg/);
  if (kg) return Number(kg[1]) * 1000;

  // L / Liter: "1.5l", "1 l"
  const liter = q.match(/(\d+(\.\d+)?)\s*(l|liter|liters)/);
  if (liter) return Number(liter[1]) * 1000;

  // Grams: "50g", "70 g", "1 pack (70g)"
  const grams = q.match(/(\d+(\.\d+)?)\s*g/);
  if (grams) return Number(grams[1]);

  // Ml: "500ml", "250 ml"
  const ml = q.match(/(\d+(\.\d+)?)\s*ml/);
  if (ml) return Number(ml[1]);

  // Plain number fallback
  const num = q.match(/(\d+(\.\d+)?)/);
  if (num && Number(num[1]) > 0) return Number(num[1]);

  return 100;
}

// ── ADD FOOD ─────────────────────────────────────────────────────────────────
app.post('/add-food', async (req, res) => {
  try {
    const { uid, foodName, calories, protein, carbs, fat, imageUrl, quantity, unit } = req.body;

    if (!uid || !foodName) {
      return res.status(400).json({ error: 'uid and foodName are required' });
    }

    const docRef = await db.collection(COLLECTION).add({
      uid,
      foodName,
      calories: Math.round(Number(calories)) || 0,
      protein: Number(protein) || 0,
      carbs: Number(carbs) || 0,
      fat: Number(fat) || 0,
      imageUrl: imageUrl || '',
      quantity: Number(quantity) || 1,
      unit: unit || 'serving',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.json({ message: 'Success', id: docRef.id });
  } catch (err) {
    console.error('add-food:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── GET FOODS (today only) ───────────────────────────────────────────────────
app.get('/get-foods/:uid', async (req, res) => {
  try {
    const snapshot = await db.collection(COLLECTION)
      .where('uid', '==', req.params.uid)
      .get();

    const foods = [];
    snapshot.forEach(doc => {
      const d = doc.data();
      const ts = d.createdAt ? d.createdAt.toMillis() : Date.now();

      // Only return today's foods
      const today = new Date();
      const docDate = new Date(ts);
      const isSameDay =
        docDate.getDate() === today.getDate() &&
        docDate.getMonth() === today.getMonth() &&
        docDate.getFullYear() === today.getFullYear();

      if (isSameDay) {
        foods.push({ id: doc.id, ...d, _ts: ts });
      }
    });

    foods.sort((a, b) => b._ts - a._ts);
    res.json(foods);
  } catch (err) {
    console.error('get-foods:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── DELETE FOOD ──────────────────────────────────────────────────────────────
app.delete('/delete-food/:id', async (req, res) => {
  try {
    await db.collection(COLLECTION).doc(req.params.id).delete();
    res.json({ message: 'Deleted' });
  } catch (err) {
    console.error('delete-food:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── SEARCH FOOD ──────────────────────────────────────────────────────────────
app.get('/search-food', async (req, res) => {
  try {
    const { name, quantity = 1, unit = 'serving' } = req.query;
    if (!name) return res.status(400).json({ error: 'name is required' });

    let nutrients = { calories: 0, protein: 0, carbs: 0, fat: 0 };
    let resultFoodName = name;
    let imageUrl = '';

    // 1. Try USDA Search
    let food = null;
    try {
      const usdaRes = await axios.get(
        `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(name)}&api_key=${process.env.USDA_API_KEY}&pageSize=1`
      );
      food = usdaRes.data.foods?.[0];
      if (food) {
        resultFoodName = food.description;
        food.foodNutrients.forEach(n => {
          const nid = Number(n.nutrientId);
          const nname = (n.nutrientName || '').toLowerCase();
          if ([1008, 2047, 2048, 208].includes(nid) || nname.includes('energy') || nname.includes('calorie')) {
            if (n.unitName === 'KCAL' || nname.includes('kcal') || !nutrients.calories) {
              if (n.value > 0) nutrients.calories = n.value;
            }
          }
          if (nid === 1003 || nid === 203 || nname.includes('protein')) {
            if (n.value > 0) nutrients.protein = n.value;
          }
          if (nid === 1005 || nid === 205 || nname.includes('carbohydrate') || nname.includes('carb')) {
            if (n.value > 0) nutrients.carbs = n.value;
          }
          if (nid === 1004 || nid === 204 || nname.includes('lipid') || nname.includes('fat')) {
            if (n.value > 0) nutrients.fat = n.value;
          }
        });
      }
    } catch (e) {
      console.error('USDA search error:', e.message);
    }

    // 2. Fallback to OpenFoodFacts if USDA has missing nutrients or no match
    if (!food || (nutrients.protein === 0 && nutrients.carbs === 0)) {
      try {
        const offRes = await axios.get(
          `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(name)}&search_simple=1&action=process&json=1&page_size=1`
        );
        const offProduct = offRes.data?.products?.[0];
        if (offProduct) {
          const n = offProduct.nutriments || {};
          const quantityText = offProduct.serving_size || offProduct.quantity || offProduct.product_quantity || '100g';
          const offGrams = extractGrams(quantityText);

          let cal100 = getNutrientVal(n, ['energy-kcal_100g', 'energy-kcal_value', 'energy-kcal', 'energy_100g']);
          if (cal100 > 0 && n['energy_100g'] && !n['energy-kcal_100g'] && !n['energy-kcal_value']) {
            cal100 = Math.round(cal100 / 4.184);
          }

          let p100 = getNutrientVal(n, ['proteins_100g', 'proteins_value', 'proteins']);
          let c100 = getNutrientVal(n, ['carbohydrates_100g', 'carbohydrates_value', 'carbohydrates']);
          let f100 = getNutrientVal(n, ['fat_100g', 'fat_value', 'fat']);

          if (cal100 > 0) {
            nutrients.calories = calculateNutrition(cal100, offGrams);
            nutrients.protein = calculateNutrition(p100, offGrams);
            nutrients.carbs = calculateNutrition(c100, offGrams);
            nutrients.fat = calculateNutrition(f100, offGrams);
          } else {
            nutrients.calories = Math.round(getNutrientVal(n, ['energy-kcal_serving', 'energy_serving']));
            if (n['energy_serving'] && !n['energy-kcal_serving']) nutrients.calories = Math.round(nutrients.calories / 4.184);
            nutrients.protein = getNutrientVal(n, ['proteins_serving']);
            nutrients.carbs = getNutrientVal(n, ['carbohydrates_serving']);
            nutrients.fat = getNutrientVal(n, ['fat_serving']);
          }

          if (offProduct.image_url || offProduct.image_front_url) {
            imageUrl = offProduct.image_url || offProduct.image_front_url;
          }
          resultFoodName = offProduct.product_name || offProduct.product_name_en || name;
        }
      } catch (e) {
        console.error('OFF search fallback error:', e.message);
      }
    }

    // Calculate final grams based on unit & quantity requested
    let cleanName = name.toLowerCase().split(' ')[0].replace(/s$/, '');
    let grams = 100;
    if (unit === 'g') grams = Number(quantity);
    else if (unit === 'kg') grams = Number(quantity) * 1000;
    else if (unit === 'ml') grams = Number(quantity);
    else if (unit === 'pieces') grams = Number(quantity) * (pieceWeights[cleanName] || 100);
    else grams = Number(quantity) * 100; // serving

    // Fetch Pixabay image if missing
    if (!imageUrl) {
      try {
        const cleanQ = name.split(',')[0].split(' ')[0];
        const pixRes = await axios.get(
          `https://pixabay.com/api/?key=${process.env.PIXABAY_API_KEY}&q=${encodeURIComponent(cleanQ + ' food')}&image_type=photo&category=food&safesearch=true&per_page=3`
        );
        if (pixRes.data.hits?.length > 0) imageUrl = pixRes.data.hits[0].webformatURL;
      } catch (e) {}
    }

    res.json({
      name: resultFoodName,
      calories: calculateNutrition(nutrients.calories, grams),
      protein: calculateNutrition(nutrients.protein, grams),
      carbs: calculateNutrition(nutrients.carbs, grams),
      fat: calculateNutrition(nutrients.fat, grams),
      imageUrl,
    });
  } catch (err) {
    console.error('search-food:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── BARCODE ──────────────────────────────────────────────────────────────────
app.get('/barcode/:code', async (req, res) => {
  try {
    const r = await axios.get(
      `https://world.openfoodfacts.org/api/v0/product/${req.params.code}.json`
    );

    if (r.data.status === 0 || !r.data.product) return res.status(404).json({ error: 'Product not found' });

    const p = r.data.product;
    const quantityText = p.serving_size || p.quantity || p.product_quantity || '100g';
    const grams = extractGrams(quantityText);
    const n = p.nutriments || {};

    // 1. Try per-100g values first
    let cal100 = getNutrientVal(n, ['energy-kcal_100g', 'energy-kcal_value', 'energy-kcal', 'energy_100g']);
    if (cal100 > 0 && n['energy_100g'] && !n['energy-kcal_100g'] && !n['energy-kcal_value']) {
      // Convert kJ -> kcal if energy_100g is in kJ
      cal100 = Math.round(cal100 / 4.184);
    }

    let p100 = getNutrientVal(n, ['proteins_100g', 'proteins_value', 'proteins']);
    let c100 = getNutrientVal(n, ['carbohydrates_100g', 'carbohydrates_value', 'carbohydrates']);
    let f100 = getNutrientVal(n, ['fat_100g', 'fat_value', 'fat']);

    let totalCals = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0;

    if (cal100 > 0) {
      // Calculate packet proportion based on net grams
      totalCals = Math.round(calculateNutrition(cal100, grams));
      totalProtein = calculateNutrition(p100, grams);
      totalCarbs = calculateNutrition(c100, grams);
      totalFat = calculateNutrition(f100, grams);
    } else {
      // Fallback to per-serving values directly if 100g values are missing
      totalCals = Math.round(getNutrientVal(n, ['energy-kcal_serving', 'energy_serving']));
      if (n['energy_serving'] && !n['energy-kcal_serving']) totalCals = Math.round(totalCals / 4.184);
      totalProtein = getNutrientVal(n, ['proteins_serving']);
      totalCarbs = getNutrientVal(n, ['carbohydrates_serving']);
      totalFat = getNutrientVal(n, ['fat_serving']);
    }

    const unitType = (String(quantityText).toLowerCase().includes('ml') || String(quantityText).toLowerCase().includes('l')) ? 'ml' : 'g';

    res.json({
      name: p.product_name || p.product_name_en || 'Unknown Item',
      calories: totalCals,
      protein: totalProtein,
      carbs: totalCarbs,
      fat: totalFat,
      quantity: grams,
      unit: unitType,
      imageUrl: p.image_url || p.image_front_url || '',
    });
  } catch (err) {
    console.error('barcode:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── AI IMAGE ANALYSIS (Gemini Flash) ─────────────────────────────────────────
app.post('/analyze-image', aiLimiter, upload.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No image uploaded' });
    }

    const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
    if (!GEMINI_API_KEY) {
      return res.status(500).json({ error: 'AI service not configured' });
    }

    const imageBase64 = req.file.buffer.toString('base64');
    const mimeType = req.file.mimetype || 'image/jpeg';

    const prompt = `You are a nutrition expert AI. Analyze this food image and identify the food item(s) visible.

Respond with ONLY a valid JSON object in exactly this format (no extra text, no markdown):
{
  "name": "food name (be specific, e.g. 'Grilled Chicken Breast' not just 'chicken')",
  "calories": <number, estimated total calories in the visible portion>,
  "protein": <number, grams of protein>,
  "carbs": <number, grams of carbohydrates>,
  "fat": <number, grams of fat>,
  "confidence": "<high|medium|low>"
}

If you cannot identify food in the image, return:
{"name":"Unknown Food","calories":0,"protein":0,"carbs":0,"fat":0,"confidence":"low"}

Base estimates on a typical single serving portion visible in the image.`;

    const models = ['gemini-1.5-flash', 'gemini-2.0-flash', 'gemini-1.5-pro'];
    let rawText = '';
    let lastError = null;

    for (const model of models) {
      try {
        const geminiRes = await axios.post(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`,
          {
            contents: [{
              parts: [
                { text: prompt },
                {
                  inlineData: {
                    mimeType: mimeType,
                    data: imageBase64,
                  }
                }
              ]
            }],
            generationConfig: {
              temperature: 0.2,
              maxOutputTokens: 300,
            }
          },
          { headers: { 'Content-Type': 'application/json' }, timeout: 15000 }
        );

        rawText = geminiRes.data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
        if (rawText) break;
      } catch (err) {
        console.error(`Gemini model ${model} error:`, err.response?.data?.error?.message || err.message);
        lastError = err;
      }
    }

    if (!rawText && lastError) {
      throw lastError;
    }

    console.log('Gemini raw response:', rawText);

    // Extract JSON from response (handles markdown code blocks too)
    const jsonMatch = rawText.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      return res.status(422).json({ error: 'Could not parse AI response' });
    }

    const parsed = JSON.parse(jsonMatch[0]);

    res.json({
      name: String(parsed.name || 'Unknown Food'),
      calories: Number(parsed.calories) || 0,
      protein: Number(parsed.protein) || 0,
      carbs: Number(parsed.carbs) || 0,
      fat: Number(parsed.fat) || 0,
      imageUrl: '',
      confidence: String(parsed.confidence || 'medium'),
    });

  } catch (err) {
    console.error('analyze-image error:', err.response?.data || err.message);
    if (err.response?.status === 429) {
      return res.status(429).json({ error: 'AI service busy. Please try again.' });
    }
    const errMsg = err.response?.data?.error?.message || err.message || 'Image analysis failed.';
    res.status(500).json({ error: errMsg });
  }
});

// ── Health Check ─────────────────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// ── Start Server ─────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`✅ Calorimeter backend running on port ${PORT}`);
});
