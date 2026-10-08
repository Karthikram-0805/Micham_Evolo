package com.example.domain.engine

import java.time.LocalTime
import kotlin.random.Random

object DialogueEngine {

    private var lastReaction: String = ""
    private var lastGreeting: String = ""

    // 30 Small Expense Reactions (< 0.2% of salary)
    private val SMALL_EXPENSE = listOf(
        "₹%s ah? Seri... tea nu nenachukalam 😌",
        "₹%s pocha? Paravala manageable damage da 😎",
        "Idhellam oru expense-ah? Chill pannu bro ☕",
        "Chillar matter da! Tension aagadhe 🪙",
        "Tea + biscuit cost dhaan idhu, safe zone 🍪",
        "Manageable damage... wallet perusa azhala 😌",
        "Indha maadhiri expense-ku permission illama spend pannalam 👍",
        "₹%s dhaane... indha vaati unakku pass 😂",
        "Pocket money level dhaan idhu, no tension ✨",
        "Kutti expense, kutti heart attack illa ✌️",
        "Tea kadai bill madhiri iruku, paravala ☕",
        "₹%s spend pannadhukku accountant theva illa 😌",
        "Sotta sotta dhaan kulam aagum, aana idhu safe dhaan 💧",
        "Idhukkellam shock aana epdi? Go ahead 😎",
        "Small damage, big smile... manage aagidum 😄",
        "₹%s... daily allowance la kooda adangidum 😌",
        "Wallet-la irundhu chinna dust madhiri poiruku 🍃",
        "No problem da, indha expense-ah marandhudu ✌️",
        "Oru cup tea kudichadhu pola nenachuko ☕",
        "Indha range la spend panna monthly survive aayidalam 👍",
        "Damage level: very minimal 🛡️",
        "Wallet: '₹%s ah? Idhellam naane paathukaren' 😌",
        "Chinna expense, periya nimmadhi 😇",
        "Seri seri, idhukku kooda note podanum ah nu yosikkaadha 📝",
        "Kavala padaadha bro, wallet safe dhaan 🛡️",
        "₹%s out... aana balance strong-ah iruku 💪",
        "Idhu expense category-la kooda varadhu, chill 🍧",
        "Cute little expense, pathiram-ah iru 🐥",
        "Wallet didn't even notice this one 😂",
        "Sari vidu da, oru samosa saptadhu pola 🥟"
    )

    // 30 Medium Expense Reactions (0.2% - 1% of salary)
    private val MEDIUM_EXPENSE = listOf(
        "₹%s pocha? Purse konjam azhudhu pola 😭",
        "Normal speed dhaan, steady-ah po bro 🚶",
        "Swiggy la oru meal mudinjiduchu pola 👀",
        "₹%s out! Konjam observe pannanum 🧐",
        "Purse light-ah shock aaiduchu da ⚡",
        "Indha expense konjam noticeable damage thaan ⚠️",
        "₹%s ah? Seri aana adutha round yosichu spend pannu 🤔",
        "Wallet konjam weight koraunjiduchu 🏃",
        "Manage pannalam, aana caution required 🚦",
        "Oru cinema ticket with popcorn level damage 🍿",
        "₹%s swipe aachu... OTP pathi yosichiya? 📲",
        "Inniku expense chart la idhu star performer 🌟",
        "Paravala, aana indha habit continue aaga koodadhu 👀",
        "Pocket la chinna kaathu adikkuthu 💨",
        "₹%s out! Salary countdown starts now ⏳",
        "Konjam disciplined-ah irukalam da 🫡",
        "Weekend spend madhiri iruku, balance paathuko 🗓️",
        "Purse: 'Enna thideer nu ivlo koraunjuten?' 🥺",
        "₹%s ah? Paravala, aana emergency fund thodaadhe 🚨",
        "Caution bro, idhu madhiri innum rendu vandha trouble 🚧",
        "Moderate damage recorded in server 🖥️",
        "Un shopping urge konjam activate aaiduchu pola 🛍️",
        "Nalla dhaan poitu irundhudhu... idhu enna expense? 😂",
        "₹%s gone! Wallet speed breaker la erangudhu 🚸",
        "Seri, ippo konjam water kudichitu rest edu 💧",
        "GPay sound ketathum heart beat increase aacha? 💓",
        "Control bro, week start-la ye ipdi spend panna mudiyuma? 📅",
        "₹%s... safe boundary la iruka, cross aagaadha 🚧",
        "Wallet-ku oru minute mouna anjali 🥀",
        "Nalla expense dhaan, aana adutha 2 days save pannu 🧘"
    )

    // 30 High Expense Reactions (1% - 3% of salary)
    private val HIGH_EXPENSE = listOf(
        "₹%s ah? Idhu expense illa boss... event-u 😂",
        "Konjam yosichu spend panni irukalam da 👀",
        "Purse nalla shake aachu da indha vaati 🫨",
        "₹%s out! Indha speed la pona month end kashtam 🏃",
        "Wallet-la red alert siren adikidhu 🚨",
        "Bro... idhu theva thaana nu un manasa ketuko 🤔",
        "Salary graph la oru periya dip vandhuduchu 📉",
        "₹%s pochu! Inime 3 days curd rice dhaan 🍚",
        "Amazon cart la edho periya kaand pannita pola 📦",
        "Purse ICU ward ku move aagudhu bro 🏥",
        "Inniku damage score 10/10 😂",
        "₹%s ah? Heart attack trailer paatha feel 💔",
        "Un savings account unna nalla moraikidhu 👁️",
        "Heavy damage detected! Caution alert! 💥",
        "Idhukku approm un daily budget crash aagum pola 💥",
        "₹%s out! Wallet scream pannudhu kekudha? 😱",
        "Salary credit aana feel ippo totally gone 🫠",
        "Oru second un balance paathuko... bayama iruka? 🫣",
        "₹%s ah? Party mudinjiduchu, bill vandhuduchu 🎉",
        "Indha expense paathu un mom shock aavangale 👵",
        "Purse la hole perusa aaiduchu da 🕳️",
        "Bro, budget breakdown aaiduchu, mechanic kooda fix panna mudiyadhu 🔧",
        "₹%s gone with the wind 🍃💸",
        "Innum 20 days iruku month mudiyara varaikkum... ninaivu iruka? 📆",
        "GPay button press pannumbodhu kai nadungaliya? 🥶",
        "Indha expense ku periya heart venum boss 🫀",
        "₹%s! Micham evlo nu ippo paaru, shock aava ⚡",
        "Adutha vaaram strictly pocket-la kai vekka koodadhu 🤐",
        "Damage heavy! Insurance claim panna mudiyuma? 📑",
        "Purse: 'Thalaiva... podhum thalaiva!' 😭"
    )

    // 30 Dangerous Spending Reactions (> 3-5%+ of salary)
    private val DANGEROUS_EXPENSE = listOf(
        "Enna bro... salary-ku personal revenge ah? 💀",
        "Indha expense pathi police complain kudukalama? 😂",
        "Credit card swipe panra sound heart-la kekudhu 💔",
        "₹%s ah?! Boss... un balance ippo coma-la iruku 💀",
        "Salary-ku funeral arrange pannalama? ⚰️",
        "₹%s out! Adutha maasam varaikkum fasting dhaan 🥣",
        "Idhu expense illa, un bank account ku tsunami 🌊",
        "Purse totally destroyed! Game over man 🎮",
        "Bro... indha speed la pona loan dhaan apply pannanum 🏦",
        "₹%s spend pannita... ippo Micham Evlo nu ketka kooda bayama iruku 😭",
        "Bank server kooda error message anupudhu pola ⚠️",
        "Bro is spending like a billionaire on ₹50k salary 👑💸",
        "Purse: 'Naan enna unakku edhiriya da?' 😭",
        "₹%s pochu... Adutha 2 weeks breathing only air 🌬️",
        "Un wallet suicide note ezhudhiduchu 📝💀",
        "Critical hit! 99% HP damage to wallet 🛡️💥",
        "Enna bro lottery adichudha? Illana yen indha thillu? 🎰",
        "₹%s?! Idhukku unakku GST exemption theva padum 🧾",
        "Wallet weeping in the corner right now 😭",
        "Ippo dhaan real financial thriller start aagudhu 🍿",
        "₹%s out! Sombu thooki train la poga vendiyadhu dhaan 🚂",
        "Bro, un financial advisor unna resign pannirupaaru 🏃",
        "Adutha maasam salary credit aagum bodhu dhaan kannu thorakanum 🙈",
        "Damage meter exploded! 💥💥💥",
        "₹%s ah? Month end la pazhaya paper dhaan vikanum 📰",
        "Wallet flattened like parotta 🫓",
        "This is not spending, this is emotional damage 😭",
        "Bro... bank app-ah uninstall pannidu, paatha azhudhuduva 🙈",
        "₹%s! Micham irukkura kaasu kooda escape aaga paakkudhu 🏃‍♂️",
        "RIP Wallet... May your soul rest in peace 🪦"
    )

    // 30 Low Balance Reactions
    private val LOW_BALANCE = listOf(
        "Micham paatha naanum shock aiten da 😭",
        "Micham Evlo nu app pera vechadhu ippo dhaan puriyuthu 💀",
        "Salary mudinjiduchu... aana month mudiyala 😭",
        "Wallet ICU pakkam poguthu bro 🏥",
        "Yellow alert illa bro, red alert siren adikidhu 🚨",
        "Micham balance paaka kooda dhairiyam venum 🙈",
        "Purse la kaathu dhaan iruku, kaasu illa 💨",
        "Inime Swiggy paatha app close panniru 📱",
        "Bro, balance single digit ku pogama paathuko 🔢",
        "Wallet: 'Innum yen enkitta ethir paakura?' 🥺",
        "Bank balance pathi yosicha BP yerudhu 🩸",
        "Month end survivor mode activated 🛡️",
        "₹0 micham aaga innum konjam dhaan baaki ⏳",
        "Indha month ku bye bye solla ready aagiko 👋",
        "Fasting season officially started 🥣",
        "Balance la minus sign varama paathuko bro ➖",
        "Pocket la coin sound kooda kekala, silent mode 🔕",
        "Purse empty... aana manasu niraiva iruku (illana azhudhudalaam) 😭",
        "Micham irukkura kaasa locker la vachu poottu 🔐",
        "Inime coffee illa, hot water dhaan ☕❌",
        "Salary speed-a vida expense supersonic jets 🚀",
        "Bro unakku immediate-ah additional income theva 💰",
        "Balance card paaka bayama iruka? Enakkum dhaan 🫣",
        "Adutha 10 days survival challenge da 🏕️",
        "Purse la spider web kattiruku 🕸️",
        "Pazhaiya 1 rupee coin edhavadhu pant pocket la iruka nu thedu 🔍",
        "Micham evlo? Konjam dhaan da... azhadha 🫂",
        "Salary-ku retirement kuduthutiya already? 👴",
        "Warning: Wallet breathing its last breaths 🫁",
        "Sathiyama solren, inime oru tea kooda spend pannaadhe 🤐"
    )

    // 20 Positive Saving Reactions (> 75% balance remaining)
    private val POSITIVE_SAVING = listOf(
        "Semma control 😎 Idhe speed la maintain pannu!",
        "Mass panra da, savings guru un kitta dhaan kathukanum 🧘",
        "Wallet safe zone la periya smile oda iruku 😄",
        "Salary intact! Confidence level 100% 💯",
        "Discipline level: Pro Max! Keep it up 🏆",
        "Micham nalla iruku boss, super financial management 📈",
        "Inime unakku personal finance award kudukanum 🎖️",
        "Wallet: 'Nee oru nalla manushan da' 😇",
        "Great going! Month end la nalla micham nikkum 💰",
        "Paisa vasool control! Semma discipline da 👏",
        "Purse fat-ah iruku, dieting theva illa 💼",
        "Savings rocket speed la erudhu 🚀",
        "Super star madhiri manage panra da finance-ah ⭐",
        "Control pathu Warren Buffett kooda proud aavaru 🕶️",
        "Wallet green zone la chill pannudhu 🟢",
        "Good going bro! Savings target easy-ah reach pannidalam 🎯",
        "Financial peace of mind feels good, right? 😌",
        "Un future self unakku thanks solvaan da 🙏",
        "Clean and safe spending! Respect 🫡",
        "Micham paathu heart romba happy-ah iruku 💚"
    )

    // 20 No-Spend-Day Reactions
    private val NO_SPEND_DAY = listOf(
        "Inniku romba discipline-ah iruka pola 👀",
        "Inniku wallet holiday ah? Semma da 😌",
        "Zero spend today! Mass hero feeling 🦸",
        "Wallet today: 'Enakku full rest kudutha dheyvam' 😴",
        "Inniku kaasu safe, manasu nimmadhi 😇",
        "No expense recorded! Un wallet unna bless pannudhu 🧘",
        "Discipline day! Inniku oru tea kooda velila kudikkalaya? ☕",
        "Day finished with zero damage! Celebrate pannu 🎉",
        "Zero spending streak! Idhe continue pannu 🏃",
        "GPay app open kooda aagala pola iniku, awesome 👏",
        "Wallet: 'Inniku naan happy-ah thoonguven' 💤",
        "Purse intact! Masterclass in self-control 🎓",
        "Inniku un savings account sirikidhu 😄",
        "Zero damage day! Rating 5/5 ⭐⭐⭐⭐⭐",
        "Clean sheet maintained like a pro goalkeeper 🧤",
        "Pocket la kaasu appadiye iruku! Semma feel 💎",
        "Inniku spending freeze! Brilliant move 🧊",
        "Bro resisted all online sales today, hats off 🎩",
        "Zero expenses = 100% peace of mind ☮️",
        "Nalla control da, idhe madhiri innum 5 days irundha gethu 👑"
    )

    // 20 Payday / Salary Added Reactions
    private val PAYDAY_REACTIONS = listOf(
        "Salary vandhuduchu! Raja madhiri feel aagudhu 👑",
        "Pana mazhai! Aana konjam control-ah iru da ☔",
        "Account balance paathu kannula aanandha kanneer 🥹",
        "Credit alert sound ketathum adrenaline rush! 🔔",
        "Salary is here! Welcome to the richest day of the month 💰",
        "Rich kid for 24 hours mode activated 😎",
        "Kaasu vandhachu! Aana odane Amazon open pannaadhe 📦",
        "Purse full-ah fill aaiduchu! Breath of fresh air 💨",
        "King of the wallet! Aana budget pottu spend pannu 👑",
        "Salary credit: Heart rate returned to normal 🩺",
        "Semma feeling! Indha maasam achum micham vaikka paaru 🎯",
        "Fresh salary, fresh hopes! All the best da 🌟",
        "Balance looking handsome today 😍",
        "Payday mood: Everything feels possible 🚀",
        "Kadavul irukaar da! Salary vandhuduchu 🙏",
        "Purse rechargeable battery 100% full 🔋",
        "Account: 'Feed accepted! Thank you' 🤖",
        "Super da! Plan panni spend panna month end safe 📊",
        "Salary vandha gethu vera level dhaan 🔥",
        "Vandhuduchu da! Aana Micham Evlo nu month end la paakanum 👀"
    )

    // Roast Mode Spicy Reactions
    private val ROAST_REACTIONS = listOf(
        "Un wallet-ku nee dhaan villain pola da 💀",
        "Bank balance unna block panna yosichitu iruku bro 🚫",
        "Salary credit aana speed vida spend speed supersonic 🚀💸",
        "Bro is spending like he owns the Reserve Bank 😂",
        "Kaasu irundha odane erichudanum nu edhavadhu vow eduthiya? 🔥",
        "Un account statement paatha horror movie thothu pogum 👻",
        "Purse un mela court-la case poda ready-ah iruku ⚖️",
        "Bro... next month salary varaikkum oxygen mattum swaasi 🌬️",
        "Un kitta credit card kuduthadhu periya thappu da 💳🤦",
        "Micham evlo nu paatha app-ye hang aayidum pola 💀",
        "ATM machine kooda unna paatha shutter moodidum 🏧🚪",
        "Spending master, savings disaster! 😂",
        "Bank manager un photo-va office la dart board la vachirupaaru 🎯",
        "Indha expense paathu un ancestors kooda facepalm pannuvanga 🤦‍♂️",
        "Purse-ku emergency oxygen cylinder mathunga da 🤿"
    )

    fun getExpenseReaction(
        amount: Double,
        monthlySalary: Double,
        remainingBalance: Double,
        isRoastMode: Boolean = false
    ): String {
        val formattedAmount = formatSimple(amount)
        val salaryRatio = if (monthlySalary > 0) amount / monthlySalary else 0.05
        val balanceRatio = if (monthlySalary > 0) remainingBalance / monthlySalary else 0.1

        val list = when {
            isRoastMode && (salaryRatio > 0.01 || Random.nextBoolean()) -> ROAST_REACTIONS
            balanceRatio < 0.10 -> LOW_BALANCE
            salaryRatio < 0.002 -> SMALL_EXPENSE
            salaryRatio < 0.010 -> MEDIUM_EXPENSE
            salaryRatio < 0.030 -> HIGH_EXPENSE
            else -> DANGEROUS_EXPENSE
        }

        return pickRandomNonRepeating(list, formattedAmount)
    }

    fun getGreeting(remainingPercent: Int, todaySpend: Double, isRoastMode: Boolean): String {
        val hour = LocalTime.now().hour
        val timeGreeting = when {
            hour in 5..11 -> "Good morning da ☀️"
            hour in 12..15 -> "Lunch saptiya? Expense add panna marandhudaadha 👀"
            hour in 16..20 -> "Evening vandhuduchu... wallet safe-ah? 🌆"
            else -> "Inniku evlo damage pannom nu paakalama? 😂"
        }

        val statusDialogue = when {
            isRoastMode -> "Roast Mode On! Un spending thappu panna odane suthiduven 😂"
            remainingPercent > 80 -> "Wallet full-ah iruku... confidence over-ah pogadha 😏"
            remainingPercent in 50..80 -> "Nalla dhaan poitu iruku... Amazon open panna mattum vendaam 😂"
            remainingPercent in 25..49 -> "Konjam serious-ah expense paakara stage vandhuduchu 👀"
            remainingPercent in 10..24 -> "App open pannadhe brave move dhaan bro 😭"
            remainingPercent in 0..9 -> "Balance paaka vandhiya? Real courage iruku bro 💀"
            remainingPercent < 0 -> "Salary-a vida expenses fast-ah odudhu 🏃‍♂️💸"
            todaySpend == 0.0 -> "Inniku wallet holiday ah? 😌"
            else -> "Inniku mattum konjam over-ah poitom pola 👀"
        }

        return "$timeGreeting\n$statusDialogue"
    }

    fun getDailyBudgetWarning(): String {
        return "Inniku quota mudinjiduchu boss 😂 Inime spend panna penalty!"
    }

    fun getPredictionMessage(projectedRemaining: Double, projectedSpend: Double, salary: Double): String {
        return if (projectedRemaining >= 0) {
            "At this speed, month end-la roughly ₹${formatSimple(projectedRemaining)} micham irukum 😎"
        } else {
            "At this speed salary month end varaikum survive aagadhu boss 😭"
        }
    }

    fun getSavingsGoalMessage(savedSoFar: Double, goal: Double): String {
        val diff = goal - savedSoFar
        return if (diff <= 0) {
            "Savings goal achieved! Mass hero da nee! 🏆🎉"
        } else {
            "₹${formatSimple(diff)} dhaan baaki... indha month shopping app-ah konjam ignore pannalam 😎"
        }
    }

    fun getCategoryBudgetWarning(categoryName: String, percentUsed: Int): String {
        return when {
            percentUsed >= 100 -> "$categoryName budget 100% over! Stop panra da saami! 🛑"
            percentUsed >= 75 -> "$categoryName budget $percentUsed% pochu... Swiggy/app icon-ah paakadha 👀"
            else -> "$categoryName budget safe zone la iruku 👍"
        }
    }

    fun getPaydayReaction(): String {
        return pickRandomNonRepeating(PAYDAY_REACTIONS, "")
    }

    fun getNoSpendReaction(): String {
        return pickRandomNonRepeating(NO_SPEND_DAY, "")
    }

    private fun pickRandomNonRepeating(list: List<String>, amountStr: String): String {
        val candidates = list.filter { it != lastReaction }
        val template = if (candidates.isNotEmpty()) {
            candidates[Random.nextInt(candidates.size)]
        } else {
            list[Random.nextInt(list.size)]
        }
        val result = if (template.contains("%s")) {
            template.replace("%s", amountStr)
        } else {
            template
        }
        lastReaction = template
        return result
    }

    private fun formatSimple(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format("%.0f", amount)
        }
    }
}
