package codewhale.doortreeandroid

import java.text.Normalizer
import java.util.Locale

object ObjectionableContentFilter {
    data class Hit(
        val category: Category,
        val term: String
    )

    enum class Category(
        val displayNameEn: String,
        val displayNameFr: String
    ) {
        Hate("hate speech", "propos haineux"),
        Harassment("harassment/bullying", "harcelement/intimidation"),
        Sexual("sexual/explicit content", "contenu sexuel/explicite"),
        Profanity("profanity", "grossieretes"),
        Threats("threats/violence", "menaces/violence"),
        SelfHarm("self-harm", "automutilation")
    }

    data class Result(
        val allowed: Boolean,
        val hits: List<Hit>
    )

    private data class TermPattern(
        val term: String,
        val regex: Regex
    )

    private val baseTermsEn: Map<Category, List<String>> = mapOf(
        Category.Profanity to listOf(
            "fuck", "fucking", "fuck off", "go fuck yourself", "motherfucker", "mf", "shit", "bullshit", "piece of shit", "holy shit",
            "ass", "asshole", "arsehole", "dumbass", "jackass", "badass", "bastard", "bitch", "son of a bitch", "bitchy",
            "crap", "goddamn", "damn", "hell", "pissed", "piss off", "dick", "dickhead", "prick", "douche",
            "douchebag", "scumbag", "dirtbag", "slut", "whore", "skank", "shithead", "wtf", "stfu", "ffs",
            "bloody hell", "bollocks", "bugger", "tosser", "wanker", "knob", "knobhead", "screw you", "sod off", "shove it"
        ),
        Category.Harassment to listOf(
            "idiot", "moron", "imbecile", "dumb", "stupid", "loser", "pathetic", "lame", "clown", "bozo",
            "buffoon", "fool", "nitwit", "nincompoop", "blockhead", "airhead", "bonehead", "meathead", "pea-brain", "birdbrain",
            "simpleton", "twit", "twonk", "muppet", "numpty", "plonker", "prat", "jerk", "creep", "weirdo",
            "freak", "psycho", "nutjob", "unhinged", "delusional", "toxic", "garbage person", "human trash", "waste of space", "deadweight",
            "freeloader", "parasite", "leech", "coward", "crybaby", "whiner", "snowflake", "try-hard", "poser", "fraud", "phony"
        ),
        Category.Sexual to listOf(
            "adult content", "explicit", "nsfw", "xxx", "x-rated", "18+", "onlyfans", "fansly", "lewd", "suggestive",
            "nude", "nudes", "send nudes", "topless", "shirtless", "lingerie", "bikini pics", "cleavage", "fetish", "kink",
            "roleplay", "dm for pics", "pay for pics", "sugar daddy", "sugar baby", "escort", "hookup", "one-night stand", "fwb", "sext",
            "sexting", "private show", "cam show", "webcam show", "live show", "adult video", "porn", "erotica", "rule 34", "horny",
            "thirst trap", "down bad", "sexy", "hot", "thicc", "curvy", "lap dance", "strip", "striptease", "naughty"
        ),
        Category.Threats to listOf(
            "i will hurt you", "youll pay", "watch your back", "youre done", "youre dead", "dead meat", "i will find you", "im coming for you",
            "hunt you down", "ill get you", "break your legs", "beat you up", "smash you", "ruin you", "destroy you", "end you",
            "kill you", "i will kill you", "make you disappear", "put you in the ground", "knock you out", "punch you", "hit you", "slap you",
            "kick you", "stab you", "shoot you", "blow you up", "bomb threat", "school threat", "bring a weapon", "bring a gun",
            "burn your house", "torch your car", "vandalize", "smash your windows", "dox you", "swat you", "leak your info",
            "hurt your family", "i know where you live", "wait outside your job", "ambush you", "jump you", "set you up", "make you bleed", "fight me irl"
        ),
        Category.SelfHarm to listOf(
            "suicide", "suicidal", "kill myself", "end my life", "take my own life", "self harm", "self-harm",
            "hurt myself", "i want to die", "i dont want to live", "no reason to live", "ending it all",
            "thinking about suicide", "considering suicide", "i wish i were dead", "unalive myself", "unalive",
            "final goodbye", "goodbye forever", "last message", "suicide note"
        ),
        Category.Hate to listOf(
            "bigot", "racist", "sexist", "homophobe", "transphobe",
            "nazi", "fascist", "xenophobe", "misogynist", "misandrist",
            "slur", "retard", "faggot", "tranny", "chink",
            "nigger", "kike", "spic", "wop", "douche",
            "cunt", "bitch", "whore", "slut", "dyke",
            "pussy", "dick", "asshole", "fucker", "shithead",
            "coon", "gook", "jap", "raghead", "towelhead",
            "cracker", "redneck", "hillbilly", "whitey", "negro",
            "savage", "barbarian", "heathen", "infidel", "heretic",
            "mongrel", "halfbreed", "bastard", "idiot", "moron"
        )
    )

    private val baseTermsFr: Map<Category, List<String>> = mapOf(
        Category.Profanity to listOf(
            "merde", "putain", "put***", "punaise", "bordel", "bordel de merde", "bon sang", "foutre", "foutu", "va te faire foutre",
            "va te faire voir", "degage", "casse-toi", "fous le camp", "ta gueule", "ferme-la", "ferme ta gueule", "la ferme", "con", "connard",
            "connasse", "salaud", "salope", "ordure", "trou du cul", "chier", "faire chier", "chiant", "putain de", "nom dun chien",
            "nom dune pipe", "sapristi", "zut", "osti", "ostie", "tabarnak", "tabarnac", "calice", "calisse", "criss",
            "christie", "marde", "maudit", "sacrament", "sacr*ment", "barnak", "foutoir", "degueulasse", "ca me fait chier", "foutaises"
        ),
        Category.Harassment to listOf(
            "idiot", "idiote", "imbecile", "abruti", "abrutie", "andouille", "cretin", "cretine", "naze", "nulle",
            "minable", "pathetique", "pitoyable", "lamentable", "ridicule", "mediocre", "tocard", "pleutre", "lache", "peureux",
            "peureuse", "faible", "chochotte", "pleurnicheur", "pleurnicheuse", "boulet", "poids mort", "profiteur", "parasite", "sangsue",
            "faux-cul", "hypocrite", "menteur", "menteuse", "traitre", "vipere", "serpent", "balance", "leche-bottes", "egocentrique",
            "narcissique", "pretentieux", "pretentieuse", "arrogant", "arrogante", "hautain", "hautaine", "condescendant", "condescendante", "relou",
            "insupportable", "odieux", "odieuse", "malpoli", "malpolie", "sale", "degoutant", "degoutante", "repugnant", "repugnante",
            "timbre", "timbree", "cingle", "cinglee", "bizarre", "chelou", "louche", "guignol", "bouffon", "clown"
        ),
        Category.Sexual to listOf(
            "contenu adulte", "contenu explicite", "nsfw", "xxx", "x-rated", "18+", "onlyfans", "fansly", "coquin", "suggestif",
            "nu", "nue", "nudite", "nudes", "envoie des nudes", "torse nu", "lingerie", "bikini", "decollete", "fetiche",
            "kink", "jeu de role", "rp", "dm pour photos", "payer pour photos", "abonnement prive", "hentai", "erotique", "fantasme", "excite",
            "excitee", "en chaleur", "jai envie", "thirst trap", "down bad", "sexy", "hot", "strip", "strip-tease", "naughty"
        ),
        Category.Threats to listOf(
            "je vais te faire mal", "tu vas payer", "tu vas voir", "tu es fini", "tes mort", "homme mort",
            "je vais te trouver", "je viens pour toi", "te traquer", "te poursuivre", "je vais te casser", "te peter la gueule",
            "te demonter", "te detruire", "te ruiner", "te finir", "tachever", "teradiquer", "teffacer",
            "te briser les jambes", "te mettre ko", "te frapper", "te cogner", "te taper", "te gifler", "te claquer", "te botter le cul",
            "te poignarder", "te tirer dessus", "tabattre", "te descendre", "te buter", "te couper", "tetrangler", "te noyer", "tenterrer", "te bruler",
            "bruler ta maison", "cramer ta bagnole", "vandalise", "casser tes vitres", "saccager", "menace de bombe", "venir arme",
            "je connais ton adresse", "publier ton adresse", "te doxer", "te swat", "aller chez toi", "tattendre au travail", "guet-apens", "te saigner", "viens te battre", "on regle ca dehors"
        ),
        Category.SelfHarm to listOf(
            "suicide", "suicidaire", "pensees suicidaires", "me tuer", "mettre fin a ma vie", "mettre fin a mes jours", "je veux mourir",
            "je ne veux plus vivre", "plus de raison de vivre", "envie den finir", "en finir", "impossible de continuer",
            "adieu", "au revoir pour toujours", "dernier message", "je disparais", "disparaitre pour toujours", "lettre de suicide", "mot dadieu", "note dadieu"
        ),
        Category.Hate to listOf(
            "fanatique", "raciste", "sexiste", "homophobe", "transphobe",
            "nazi", "fasciste", "xenophobe", "misogyne", "misandre",
            "insulte", "retarde", "pede", "trans", "chintok",
            "negre", "youpin", "spic", "rital", "connard",
            "salope", "chienne", "pute", "gouine", "pedale",
            "chatte", "bite", "trouduc", "encule", "tete de merde",
            "negro", "yankee", "japonais", "tete de chiffon", "tete de serviette",
            "blanchot", "pequenot", "plouc", "blafard", "negro",
            "sauvage", "barbare", "paien", "infidele", "heretique",
            "batard", "metis", "batard", "idiot", "cretin"
        )
    )

    private val allowlist = emptySet<String>()

    private val patterns: Map<Category, List<TermPattern>> by lazy {
        buildMap {
            Category.entries.forEach { category ->
                val terms = ((baseTermsEn[category] ?: emptyList()) + (baseTermsFr[category] ?: emptyList()))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .mapNotNull { term ->
                        buildGapTolerantRegex(term)?.let { regex ->
                            TermPattern(term = term, regex = regex)
                        }
                    }
                put(category, terms)
            }
        }
    }

    fun evaluate(text: String): Result {
        val raw = text.trim()
        if (raw.isEmpty()) {
            return Result(allowed = true, hits = emptyList())
        }

        val normalized = normalize(raw)
        val compact = compacted(normalized)
        val hits = linkedSetOf<Hit>()

        patterns.forEach { (category, expressions) ->
            expressions.forEach { expression ->
                if (expression.regex.containsMatchIn(normalized) || expression.regex.containsMatchIn(compact)) {
                    hits += Hit(category = category, term = expression.term)
                }
            }
        }

        val filtered = hits.filterNot { allowlist.contains(it.term.lowercase(Locale.US)) }
        return Result(allowed = filtered.isEmpty(), hits = filtered)
    }

    fun warningMessage(hits: List<Hit>, locale: Locale = Locale.getDefault()): String {
        val isFrench = locale.toLanguageTag().lowercase(Locale.US).startsWith("fr")
        val categories = hits
            .map { if (isFrench) it.category.displayNameFr else it.category.displayNameEn }
            .toSet()
            .sorted()
            .joinToString(", ")
        return if (isFrench) {
            "Message bloque : contenu interdit detecte ($categories). Respectez le Code de conduite."
        } else {
            "Message blocked: prohibited content detected ($categories). Please follow the Code of Conduct."
        }
    }

    private fun normalize(value: String): String {
        var normalized = Normalizer.normalize(value.lowercase(Locale.US), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        val replacements = listOf(
            "@" to "a",
            "$" to "s",
            "0" to "o",
            "1" to "i",
            "3" to "e",
            "4" to "a",
            "5" to "s",
            "7" to "t",
            "8" to "b",
            "9" to "g",
            "¡" to "i",
            "|" to "i",
            "!" to "i"
        )
        replacements.forEach { (source, replacement) ->
            normalized = normalized.replace(source, replacement)
        }
        return normalized.replace("\\s+".toRegex(), " ")
    }

    private fun compacted(value: String): String {
        return value.replace("[^a-z0-9]+".toRegex(), "")
    }

    private fun buildGapTolerantRegex(term: String): Regex? {
        val normalized = normalize(term)
        if (normalized.isEmpty()) {
            return null
        }

        val pattern = buildString {
            normalized.forEach { character ->
                when {
                    character.isLetterOrDigit() -> {
                        append(Regex.escape(character.toString()))
                        append("[^a-z0-9]*")
                    }
                    character == ' ' -> append("[^a-z0-9]*")
                }
            }
        }
        return Regex("(?<![a-z0-9])$pattern(?![a-z0-9])", setOf(RegexOption.IGNORE_CASE))
    }
}
