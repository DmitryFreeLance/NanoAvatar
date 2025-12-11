package com.example.nanoavatar.filters;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class FilterRegistry {

    public static final String ROOT_ID = "root";

    private final Map<String, FilterNode> nodes = new LinkedHashMap<>();

    public FilterRegistry() {
        buildTree();
    }

    private void buildTree() {
        // Root
        addNode(new FilterNode(
                ROOT_ID,
                "Главное меню",
                "",
                "",
                null
        ));

        // == Top-level categories ==
        addCategory("cat_appearance", "🙂 Внешность");
        addCategory("cat_clothes", "👗 Одежда и аксессуары");
        addCategory("cat_transform", "🎭 Перевоплощения");
        addCategory("cat_photoset", "📸 Фотосессия");
        addCategory("cat_newyear", "🎄 Новый год");
        addCategory("cat_locations", "🌍 Локации");
        addCategory("cat_fun", "🤡 Приколы");
        addCategory("cat_art", "🎨 Арт-эффекты");
        addCategory("cat_trends", "📣 Тренды");
        addCategory("cat_tools", "🛠 Инструменты");
        addCategory("cat_random_look", "🎲 Случайный лук");
        addCategory("cat_random_hair", "💇‍♂️ Случайная стрижка");
        addCategory("cat_text", "⌨️ Текстовый запрос");
        addCategory("cat_text_pro", "🚀 Текстовый запрос (PRO) 🔥🆕");
        addCategory("cat_photoroulette", "🏛 Фоторулетка");
        addCategory("cat_blackbox", "⬛ Чёрный ящик ❓");
        addCategory("cat_video", "🎬 Создание видео");
        addCategory("cat_music", "🎵 Создание музыки");
        addCategory("cat_avatar", "🧍‍♂️ Мой аватар");

        // ==== Внешность ====
        addLeaf("app_soft_glam", "✨ Софт‑глям",
                "Нежный макияж, мягкий свет и лёгкое свечение кожи — аккуратно подчёркивает красоту без перегруза.",
                "soft glam portrait, subtle makeup, smooth skin retouch, warm soft lighting, gentle cinematic glow, beauty photography",
                "cat_appearance");

        addLeaf("app_cinematic", "🎞 Кинопортрет",
                "Кинематографичный портрет с контрастным светом и объёмом, как кадр из фильма.",
                "cinematic portrait, dramatic key light, moody shadows, rich contrast, film still look",
                "cat_appearance");

        addLeaf("app_retro90", "📼 Ретро 90‑е",
                "Лёгкая зернистость, тёплый оттенок, вспышка и вайб старых фото из 90‑х.",
                "retro 90s aesthetic, on-camera flash, slight grain, warm tones, nostalgic vibe",
                "cat_appearance");

        addLeaf("app_anime", "🎌 Аниме‑портрет",
                "Стилизация под аниме: выразительные глаза, мягкие тени и аккуратный контур.",
                "anime style portrait, clean lines, large expressive eyes, soft shading, pastel colors",
                "cat_appearance");

        // ==== Одежда и аксессуары ====
        addLeaf("cl_street", "🧢 Streetwear",
                "Худи, кроссы, бейсболка — современный уличный стиль, как у модных тиктокеров.",
                "modern streetwear outfit, hoodie, sneakers, cap, urban background, casual cool vibe",
                "cat_clothes");

        addLeaf("cl_business", "💼 Бизнес‑лук",
                "Строгий костюм, рубашка, аккуратный фон — идеальный аватар для работы.",
                "business outfit, elegant suit or blazer, clean office background, professional portrait",
                "cat_clothes");

        addLeaf("cl_party", "🎉 Вечеринка",
                "Блёстки, яркий макияж и эффект клуба — будто с фотосета после тусовки.",
                "party outfit, glitter details, colorful club lights, dynamic fun atmosphere",
                "cat_clothes");

        addLeaf("cl_fantasy", "🧙 Фэнтези‑костюм",
                "Плащи, доспехи или магический наряд — ты герой своего фэнтези‑мира.",
                "fantasy costume, cloak or light armor, magical accessories, dramatic fantasy background",
                "cat_clothes");

        // ==== Перевоплощения ====
        addLeaf("tf_superhero", "🦸 Супергерой",
                "Кинокомикс‑эстетика: костюм героя, мощный фон и драматичный свет.",
                "superhero style costume, dynamic pose, dramatic lighting, city skyline background",
                "cat_transform");

        addLeaf("tf_cyberpunk", "🌆 Киберпанк",
                "Неон, дождь, вывески и отражения — как кадр из киберпанк‑аниме.",
                "cyberpunk city, neon lights, rainy reflections, futuristic tech details, moody atmosphere",
                "cat_transform");

        addLeaf("tf_elf", "🧝 Эльфийский стиль",
                "Лёгкое фэнтези с острыми ушами, декором из листьев и мягким волшебным светом.",
                "elegant elf character, subtle pointed ears, forest fantasy background, soft magical light",
                "cat_transform");

        addLeaf("tf_cartoon", "📺 Мультгерой",
                "Образ мультперсонажа с упрощёнными формами и яркими цветами.",
                "cartoon character style, simplified shapes, bold colors, playful expression",
                "cat_transform");

        // ==== Фотосессия ====
        addLeaf("ps_studio", "💡 Студийный портрет",
                "Чистый фон, аккуратный свет, лёгкий ретуш — как в фотостудии.",
                "studio portrait, softbox lighting, clean backdrop, professional retouch",
                "cat_photoset");

        addLeaf("ps_film", "🎞 Плёночная съёмка",
                "Эффект плёнки: зерно, виньетка, приглушённые цвета и атмосфера винтажа.",
                "film photography look, visible grain, subtle vignette, muted tones, nostalgic mood",
                "cat_photoset");

        addLeaf("ps_street", "🚏 Уличный стиль",
                "Городской фон, живой свет, ощущение репортажной съёмки.",
                "street photography style, real city background, natural lighting, candid vibe",
                "cat_photoset");

        addLeaf("ps_bw", "⚫️⚪️ Чёрно‑белая классика",
                "Контрастный чёрно‑белый портрет с акцентом на эмоции и свет.",
                "black and white portrait, strong contrast, dramatic lighting, timeless classic look",
                "cat_photoset");

        // ==== Новый год ====
        addLeaf("ny_sparkler", "✨ Бенгальский огонь",
                "Добавит яркие бенгальские огни и новогодние блики вокруг героя.",
                "festive sparkler lights, warm glowing particles around the subject, New Year party mood",
                "cat_newyear");

        addLeaf("ny_angel", "👼 Рождественский ангелочек",
                "Нежная рождественская атмосфера, мягкий свет и ангельские детали.",
                "soft christmas angel style, glowing halo, gentle warm light, white and gold color palette",
                "cat_newyear");

        addLeaf("ny_tree", "🎄 Ёлочка",
                "Пушистая нарядная ёлка, гирлянды и подарки, уютный зимний фон.",
                "big decorated christmas tree, fairy lights, gifts, cozy winter room background",
                "cat_newyear");

        addLeaf("ny_lights", "✨ Огоньки",
                "Много тёплых огоньков боке вокруг лица, эффект сказки.",
                "bokeh fairy lights, cinematic depth of field, warm cozy tones",
                "cat_newyear");

        addLeaf("ny_style", "🎅 Новогодний образ",
                "Полный новогодний образ: шапка, свитер и праздничное настроение.",
                "full New Year outfit, cozy sweater, Santa hat, smiling, holiday vibes",
                "cat_newyear");

        addLeaf("ny_snowfairy", "❄️ Снежная сказка",
                "Зимняя сказка: мягкий снег, морозный воздух и холодное голубоватое освещение.",
                "winter fairy tale scene, falling snow, frosty air, cool blue light, cozy warm face tones",
                "cat_newyear");

        addLeaf("ny_city", "🥂 Город в огнях",
                "Праздничный ночной город, салюты и огни мегаполиса.",
                "New Year city night, fireworks, skyscrapers with lights, festive atmosphere",
                "cat_newyear");

        // ==== Локации ====
        addLeaf("loc_beach", "🏖 Море и пальмы",
                "Песок, море, закат и пальмы — идеальный отпускной кадр.",
                "tropical beach, palm trees, sunset sky, soft golden light, vacation vibe",
                "cat_locations");

        addLeaf("loc_mountains", "🏔 Горы",
                "Скалистые горы, туман, свежий воздух и ощущение свободы.",
                "mountain landscape, misty peaks, cool air, natural light, adventure mood",
                "cat_locations");

        addLeaf("loc_citynight", "🌃 Ночной мегаполис",
                "Городские огни, подсветка витрин и отражения в лужах.",
                "big city at night, neon signs, reflections on wet asphalt, cinematic atmosphere",
                "cat_locations");

        addLeaf("loc_cafe", "☕ Уютное кафе",
                "Тёплый интерьер, лампочки, кофе и максимально уютный вайб.",
                "cozy coffee shop interior, warm lights, wooden tables, soft bokeh background",
                "cat_locations");

        addLeaf("loc_space", "🚀 Космос",
                "Герой словно в невесомости на фоне космоса и далёких галактик.",
                "space background, stars, nebulae, subtle zero gravity effect, sci-fi feel",
                "cat_locations");

        // ==== Приколы ====
        addLeaf("fun_detox", "🛏 На детоксе",
                "Ты в образе человека, который сегодня только смузи, плед и осознанность.",
                "funny detox aesthetic, cozy home clothes, smoothie in hand, relaxed but humorous mood",
                "cat_fun");

        addLeaf("fun_placepower", "🪑 Место силы",
                "Фирменное кресло, плед и лицо человека, который тут перезаряжает вселенную.",
                "special comfy chair as place of power, warm blanket, peaceful but powerful vibe",
                "cat_fun");

        addLeaf("fun_freelancer", "💼 Фрилансер",
                "Ноутбук, кофе, куча вкладок и лёгкий творческий хаос вокруг.",
                "freelancer at home, laptop, coffee mug, slightly messy cozy room, humorous tone",
                "cat_fun");

        addLeaf("fun_communicator", "📣 Коммуникатор",
                "Гуру общения: телефон, чаты, много уведомлений и мессенджеров вокруг.",
                "social media communicator, smartphone, floating chat bubbles, bright colorful UI icons",
                "cat_fun");

        addLeaf("fun_hunter", "🏹 Охотник",
                "Охотник не за зверями, а за дедлайнами. Немного эпика.",
                "epic fantasy hunter aesthetic but with office vibe, mix of armor and casual clothes",
                "cat_fun");

        addLeaf("fun_handyman", "🧰 Мастер на все руки",
                "Всё починим, всё настроим — образ универсального мастера.",
                "funny handyman style, tool belt, confident pose, lively colors, DIY background",
                "cat_fun");

        addLeaf("fun_undercover", "🕵 Под прикрытием",
                "Покажет вас в образе человека, умеющего оставаться незаметным и находить гармонию даже в самых простых условиях.",
                "undercover agent style, long coat, hat, cinematic shadows, calm confident expression",
                "cat_fun");

        addLeaf("fun_zen", "🧘 Постиг дзен",
                "Абсолютное спокойствие: подушка, свечи и лёгкая загадочная улыбка.",
                "zen meditation pose, soft warm light, candles, peaceful relaxed face",
                "cat_fun");

        addLeaf("fun_star", "🎬 Знаменитый актёр",
                "Красная дорожка, вспышки камер и ты — звезда вечера.",
                "red carpet movie star style, paparazzi flashes, glamorous lighting",
                "cat_fun");

        addLeaf("fun_vampire", "🧛 Вампив",
                "Немного тёмной эстетики и ночного настроения, клыки опциональны.",
                "stylish modern vampire, pale skin, subtle fangs, neon city night background",
                "cat_fun");

        addLeaf("fun_gap", "😊 Расщелина чудес",
                "Лёгкий сюр, странный фон и ощущение, что ты в параллельной вселенной.",
                "whimsical surreal background, playful mood, soft pastel colors, slightly dreamy",
                "cat_fun");

        addLeaf("fun_economist", "🛒 Экономист",
                "Герой, который считает каждую копейку, но делает это с улыбкой.",
                "funny economist concept, charts and graphs floating around, shopping cart or calculator",
                "cat_fun");

        addLeaf("fun_startupper", "🚀 Стартапер",
                "Толстовка, ноутбук, стикеры и ощущение, что вот‑вот взлетим в космос.",
                "startup founder vibe, hoodie, laptop with stickers, neon startup office background",
                "cat_fun");

        addLeaf("fun_word", "💪 Слово Пацана",
                "Серьёзный, уверенный взгляд и атмосфера двора — но по‑современному.",
                "confident street style, dramatic lighting, slight film grain, urban courtyard background",
                "cat_fun");

        addLeaf("fun_dumpling", "🥟 Тёщин вареник",
                "Домашний уют, еда и тёплый семейный хаос вокруг.",
                "cozy kitchen, plate of dumplings, warm family vibe, slightly humorous composition",
                "cat_fun");

        addLeaf("fun_grandpa_hose", "💦 Дедушкин шланг",
                "Летний двор, шланг, вода и немного дачного абсурда.",
                "summer backyard, garden hose spraying water, funny candid expression",
                "cat_fun");

        addLeaf("fun_energy", "⚡ На подзарядке",
                "Как будто только что выпил литр энергетика и готов покорять мир.",
                "electric energy effects, glowing lines around body, dynamic motion blur",
                "cat_fun");

        // ==== Арт-эффекты ====
        addLeaf("art_oil", "🖌 Масляная живопись",
                "Портрет, нарисованный масляными мазками в стиле классической живописи.",
                "oil painting portrait, visible brush strokes, rich textures, gallery lighting",
                "cat_art");

        addLeaf("art_watercolor", "💧 Акварель",
                "Мягкая акварель с размытыми краями и нежной палитрой.",
                "watercolor portrait, flowing paint, soft edges, pastel colors, paper texture",
                "cat_art");

        addLeaf("art_neon", "🌈 Неоновый арт",
                "Неоновые контуры, светящиеся линии и кибер‑арт атмосфера.",
                "neon outline art, glowing strokes, dark background, cyber aesthetic",
                "cat_art");

        addLeaf("art_lowpoly", "🔺 Low‑poly",
                "Геометрический портрет, собранный из треугольников.",
                "low poly portrait, faceted geometry, sharp polygons, stylized 3d look",
                "cat_art");

        addLeaf("art_pencil", "✏️ Карандашный скетч",
                "Чёрно‑белый набросок карандашом с текстурой бумаги.",
                "hand-drawn pencil sketch portrait, crosshatching, paper grain texture",
                "cat_art");

        // ==== Тренды ====
        addLeaf("tr_reels_cover", "📱 Обложка Reels",
                "Яркий вертикальный портрет с акцентом на лице и крупной подачей.",
                "vertical portrait for social media cover, bold composition, high contrast, trendy look",
                "cat_trends");

        addLeaf("tr_pinterest", "📌 Pinterest‑mood",
                "Мягкие цвета, уютный свет и композиция как на референсах из Pinterest.",
                "pinterest aesthetic, soft pastel colors, light grain, aesthetic lifestyle background",
                "cat_trends");

        addLeaf("tr_ai_glow", "✨ AI‑glow",
                "Светящийся контур вокруг лица и лёгкий фантазийный эффект.",
                "AI glow effect, soft rim light outlining face, subtle sparkles, futuristic vibe",
                "cat_trends");

        addLeaf("tr_minimal", "⚪ Минимализм",
                "Простой фон, чистые линии и аккуратный современный стиль.",
                "minimalist portrait, plain background, neutral colors, clean lines, modern design",
                "cat_trends");

        // ==== Инструменты ====
        addLeaf("tool_auto_style", "✨ Автовыбор стиля",
                "Нейросеть сама подбирает гармоничный стиль под фото пользователя.",
                "choose the most flattering and trendy style for this person automatically, keep it realistic and stylish",
                "cat_tools");

        addLeaf("tool_hd_upscale", "🔍 Повысить качество",
                "Аккуратное повышение резкости и детализации без сильных артефактов.",
                "high resolution upscaling, sharpen important details, reduce noise, keep natural skin texture",
                "cat_tools");

        addLeaf("tool_bg_remove", "🧼 Убрать фон",
                "Убираем фон и заменяем его мягким градиентом или нейтральным цветом.",
                "remove busy background and replace with smooth soft gradient studio backdrop",
                "cat_tools");

        // ==== Случайный лук ====
        addLeaf("rnd_soft", "🎲 Лёгкий рандом",
                "Случайный, но спокойный стиль для аватара без слишком жёстких эффектов.",
                "random but soft and tasteful portrait restyle, small creative details, keep it subtle",
                "cat_random_look");

        addLeaf("rnd_crazy", "🤪 Сумасшедший рандом",
                "Максимум креатива: случайные цвета, эффекты и фон, но лицо узнаваемое.",
                "wild experimental restyle, vivid colors, mixed effects, surreal background, but preserve face identity",
                "cat_random_look");

        // ==== Случайная стрижка ====
        addLeaf("hair_short", "✂️ Короткая стрижка",
                "Аккуратная короткая стрижка, подчёркивающая черты лица.",
                "short modern haircut, clean shape, hair neatly styled",
                "cat_random_hair");

        addLeaf("hair_long", "💁‍♀️ Длинные волосы",
                "Пышные длинные волосы с красивой укладкой.",
                "long voluminous hair, soft waves, well-groomed look",
                "cat_random_hair");

        addLeaf("hair_color", "🌈 Яркое окрашивание",
                "Необычный цвет волос: розовый, синий или градиент — по настроению модели.",
                "bright creative hair color, gradient dye, vivid shades like pink or blue",
                "cat_random_hair");

        // ==== Текстовые запросы ====
        addLeaf("text_simple", "🧾 Обычный промпт",
                "Ты сам задаёшь идею в подписи к фото, я аккуратно реализую её без лишних украшательств.",
                "follow the additional user text instructions from the caption exactly but keep the style realistic",
                "cat_text");

        addLeaf("text_style", "🎯 Стильный промпт",
                "Подойдёт, если хочешь чёткий визуальный результат по своему описанию.",
                "interpret the caption as detailed art-direction and create a stylish, visually strong portrait",
                "cat_text");

        addLeaf("text_pro_creative", "🔥 PRO‑креатив",
                "Максимум свободы: ты даёшь идею в подписи, а модель усиливает её и добавляет креатив.",
                "use the caption as a loose creative idea and significantly enhance it with bold artistic decisions",
                "cat_text_pro");

        addLeaf("text_pro_cinematic", "🎬 PRO‑кинокадр",
                "Из любого описания делаем кадр уровня постера к фильму.",
                "turn the caption idea into a cinematic movie-poster-like portrait, dramatic light and composition",
                "cat_text_pro");

        // ==== Фоторулетка / Чёрный ящик / Видео / Музыка / Аватар ====
        addLeaf("photoroulette_random", "🎰 Случайный фильтр",
                "Полная непредсказуемость: каждый раз новый стиль.",
                "fully random yet aesthetically pleasing style, can mix genres, keep face recognizable",
                "cat_photoroulette");

        addLeaf("blackbox_surprise", "⬛ Сюрприз‑образ",
                "Таинственный режим: модель сама решает, что с тобой сделать.",
                "mysterious experimental portrait, agent chooses style on its own, but result must look cool and shareable",
                "cat_blackbox");

        addLeaf("video_frame", "🎬 Кадр из видео",
                "Портрет, похожий на стоп‑кадр из стильного клипа.",
                "portrait styled as a frame from a stylish music video, motion blur hints, cinematic color grading",
                "cat_video");

        addLeaf("music_cover", "🎵 Обложка трека",
                "Картинка, которая подойдёт на обложку трека или плейлиста.",
                "album cover style portrait, bold typography space, strong contrast, music visual aesthetic",
                "cat_music");

        addLeaf("avatar_simple", "🧍‍♂️ Классический аватар",
                "Чистый, аккуратный портрет для мессенджеров и соцсетей.",
                "simple clean avatar portrait, centered composition, soft background, balanced colors",
                "cat_avatar");

        addLeaf("avatar_gamer", "🎮 Геймерский профиль",
                "Неон, немного киберпанка и ощущение игрового профиля.",
                "gamer avatar style, neon rim light, dark background, subtle HUD elements",
                "cat_avatar");
    }

    private void addCategory(String id, String title) {
        addNode(new FilterNode(id, title, "", "", ROOT_ID));
    }

    private void addLeaf(String id, String title, String description,
                         String promptPart, String parentId) {
        addNode(new FilterNode(id, title, description, promptPart, parentId));
    }

    private void addNode(FilterNode node) {
        nodes.put(node.getId(), node);
        if (node.getParentId() != null) {
            FilterNode parent = nodes.get(node.getParentId());
            if (parent != null) {
                parent.addChild(node.getId());
            }
        }
    }

    public FilterNode getRoot() {
        return nodes.get(ROOT_ID);
    }

    public FilterNode getNode(String id) {
        return nodes.get(id);
    }

    public Collection<FilterNode> getAllNodes() {
        return nodes.values();
    }
}