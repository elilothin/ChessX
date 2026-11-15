package com.example.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class UniverseId(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val boardName: String,
    val campaignOrder: Int, // 0 = Classique (point de départ), 1..5 = Campagne
    val aiBossName: String,
    val aiTitle: String,
    val aiDepth: Int,
    val aiRandomness: Double,
    val islandTitle: String,
    val islandDescription: String,
    val primaryColor: Color,
    val accentColor: Color,
    val lightSquareColor: Color,
    val darkSquareColor: Color,
    val boardFrameColor: Color,
    val checkmateTitle: String,
    val checkmateDescription: String,
    val resignTitle: String,
    val resignDescription: String,
    @param:DrawableRes val bannerRes: Int? = null
) {
    CLASSIQUE(
        id = "CLASSIQUE",
        displayName = "Classique",
        subtitle = "Point de départ — Bois traditionnel",
        boardName = "Échiquier en bois noble",
        campaignOrder = 0,
        aiBossName = "Maître Classique",
        aiTitle = "Gardien de l'échiquier",
        aiDepth = 1,
        aiRandomness = 0.25,
        islandTitle = "Îlot du Départ",
        islandDescription = "Un campement sobre avec un échiquier de bois sculpté, prêt pour la conquête.",
        primaryColor = Color(0xFFD4AF37),
        accentColor = Color(0xFFF3D77A),
        lightSquareColor = Color(0xFFE6D2B5),
        darkSquareColor = Color(0xFF7D5333),
        boardFrameColor = Color(0xFF3E2716),
        checkmateTitle = "Roi Renversé",
        checkmateDescription = "Le Roi s'incline et bascule dans un fondu doré solennel.",
        resignTitle = "Retrait du Roi",
        resignDescription = "Le joueur retire dignement son Roi du plateau.",
        bannerRes = null
    ),
    WESTERN(
        id = "WESTERN",
        displayName = "Western",
        subtitle = "Cow-boys, saloons en bois, duels au pistolet",
        boardName = "Saloon du Far West",
        campaignOrder = 1,
        aiBossName = "Shérif Vance",
        aiTitle = "Loi du Saloon",
        aiDepth = 2,
        aiRandomness = 0.30,
        islandTitle = "Avant-poste Western",
        islandDescription = "Un saloon en bois éclairé aux lampes à huile et cerclé de tonneaux.",
        primaryColor = Color(0xFFD97724),
        accentColor = Color(0xFFFFB74D),
        lightSquareColor = Color(0xFFD9B282),
        darkSquareColor = Color(0xFF6B3E1E),
        boardFrameColor = Color(0xFF3A1F0D),
        checkmateTitle = "Duel à Bout Portant",
        checkmateDescription = "Le Roi est encerclé au cœur du saloon et mis en joue à bout portant.",
        resignTitle = "Chapeau sur la Table",
        resignDescription = "Le joueur dépose son chapeau sur la table et quitte le saloon.",
        bannerRes = R.drawable.img_universe_western
    ),
    GRECE(
        id = "GRECE",
        displayName = "Grèce antique",
        subtitle = "Temples en marbre, mythologie et puissance divine",
        boardName = "Marbre de l'Olympe",
        campaignOrder = 2,
        aiBossName = "Stratège d'Athéna",
        aiTitle = "Champion de l'Olympe",
        aiDepth = 2,
        aiRandomness = 0.15,
        islandTitle = "Sanctuaire de Marbre",
        islandDescription = "Des colonnes ioniques blanches et des mosaïques dorées dominent l'îlot.",
        primaryColor = Color(0xFFE5C158),
        accentColor = Color(0xFF7AD7F0),
        lightSquareColor = Color(0xFFF2EFE9),
        darkSquareColor = Color(0xFF7B8794),
        boardFrameColor = Color(0xFF3E4551),
        checkmateTitle = "Foudre de l'Olympe",
        checkmateDescription = "Le Roi est figé sur son trône, foudroyé devant la puissance divine.",
        resignTitle = "Rameau d'Olivier Brisé",
        resignDescription = "Une branche d'olivier se brise lentement dans un brouillard sacré.",
        bannerRes = R.drawable.img_universe_greece
    ),
    JAPON(
        id = "JAPON",
        displayName = "Japon féodal",
        subtitle = "Samouraïs, tatamis, cerisiers et katanas",
        boardName = "Tatami & Cerisiers",
        campaignOrder = 3,
        aiBossName = "Shogun Kurogane",
        aiTitle = "Seigneur des Lames",
        aiDepth = 3,
        aiRandomness = 0.10,
        islandTitle = "Dojo des Cerisiers",
        islandDescription = "Des panneaux shoji illuminés de lanternes sous une pluie de pétales sakura.",
        primaryColor = Color(0xFFE0527B),
        accentColor = Color(0xFFFFB7C5),
        lightSquareColor = Color(0xFFE3D5A8),
        darkSquareColor = Color(0xFF4B5E42),
        boardFrameColor = Color(0xFF2B1C19),
        checkmateTitle = "Forêt de Katanas",
        checkmateDescription = "Une forêt de katanas braqués encercle le Roi vaincu sous les cerisiers.",
        resignTitle = "Salut Silencieux",
        resignDescription = "Un salut respectueux, puis une disparition silencieuse dans la brume.",
        bannerRes = R.drawable.img_universe_japan
    ),
    BASKET(
        id = "BASKET",
        displayName = "Basket-ball",
        subtitle = "Parquet, dunks aériens et buzzer-beater",
        boardName = "Parquet All-Star",
        campaignOrder = 4,
        aiBossName = "Coach Legend MVP",
        aiTitle = "Roi du Parquet",
        aiDepth = 3,
        aiRandomness = 0.04,
        islandTitle = "Arène All-Star",
        islandDescription = "Un parquet verni entouré de gradins illuminés et d'un tableau des scores.",
        primaryColor = Color(0xFFFF6D00),
        accentColor = Color(0xFFFFD180),
        lightSquareColor = Color(0xFFE8B979),
        darkSquareColor = Color(0xFF9C5221),
        boardFrameColor = Color(0xFF1F2433),
        checkmateTitle = "Contre au Buzzer",
        checkmateDescription = "Le Roi est bloqué de façon spectaculaire à l'ultime seconde avant le buzzer.",
        resignTitle = "Ballon Posé au Sol",
        resignDescription = "Le joueur dépose le ballon sur le parquet et quitte le terrain.",
        bannerRes = null
    ),
    FUTURISTE(
        id = "FUTURISTE",
        displayName = "Futuriste",
        subtitle = "Lasers, drones, cyborgs et sabres lumineux",
        boardName = "Plateau Holographique",
        campaignOrder = 5,
        aiBossName = "Noyau Omega-X",
        aiTitle = "IA Suprême de l'Armada",
        aiDepth = 3,
        aiRandomness = 0.0,
        islandTitle = "Citadelle Quantique",
        islandDescription = "Une grille holographique cyan et violette gardée par des drones en orbite.",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFFBB86FC),
        lightSquareColor = Color(0xFF1C3A4D),
        darkSquareColor = Color(0xFF2A1847),
        boardFrameColor = Color(0xFF0A101E),
        checkmateTitle = "Verrouillage Numérique",
        checkmateDescription = "Un verrouillage numérique total et des drones de surveillance capturent le Roi.",
        resignTitle = "Déconnexion de l'Avatar",
        resignDescription = "L'avatar se dissout en données et se déconnecte immédiatement.",
        bannerRes = R.drawable.img_universe_futuristic
    );

    companion object {
        val campaignSequence: List<UniverseId> = listOf(WESTERN, GRECE, JAPON, BASKET, FUTURISTE)

        fun fromId(id: String?): UniverseId =
            entries.firstOrNull { it.id == id } ?: CLASSIQUE
    }
}

data class PieceIdentity(
    val title: String,
    val identity: String,
    val attackName: String,
    val captureDescription: String,
    val reactionFamily: String // Tranché, Abattu, Écrasé, Brûlé/Désintégré, Entraîné
)

object UniverseCatalog {
    fun getPieceIdentity(universe: UniverseId, pieceType: PieceType): PieceIdentity {
        return when (universe) {
            UniverseId.CLASSIQUE -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Pion", "Fantassin classique aux doubles dagues", "Double Estoc", "Le pion avance et élimine la pièce classique d'un coup précis.", "Abattu")
                PieceType.ROOK -> PieceIdentity("Tour", "Forteresse classique au marteau de guerre", "Choc de Bastion", "La tour percute et écrase la pièce adverse.", "Écrasé")
                PieceType.KNIGHT -> PieceIdentity("Cavalier", "Chevalier armé d'une naginata", "Charge Équestre", "Le cavalier bondit par-dessus les lignes et désarçonne sa cible.", "Tranché")
                PieceType.BISHOP -> PieceIdentity("Fou", "Duelliste aux doubles katanas", "Coupe Diagonale", "Le fou fend la diagonale et tranche net sa cible.", "Tranché")
                PieceType.QUEEN -> PieceIdentity("Reine", "Souveraine de téléportation", "Frappe Royale", "La reine se téléporte derrière l'adversaire et le neutralise.", "Désintégré")
                PieceType.KING -> PieceIdentity("Roi", "Monarque au pouvoir du trou noir", "Sentence Royale", "Le roi scelle la case ennemie d'une onde gravitationnelle.", "Entraîné")
            }
            UniverseId.WESTERN -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Le chercheur d'or", "Aventurier armé d'une pioche et d'un couteau", "Crevasse Fatale", "Il se glisse derrière la cible et assène un coup de pioche au sol : l'ennemi tombe dans une crevasse.", "Entraîné")
                PieceType.ROOK -> PieceIdentity("Le saloon", "Structure lourde avec tireur embusqué au 1er étage", "Tir Winchester", "Un tir de fusil à répétition part de la fenêtre du premier étage et abat la pièce cible à distance.", "Abattu")
                PieceType.KNIGHT -> PieceIdentity("Le bandit à cheval", "Hors-la-loi masqué galopant à toute allure", "Lasso du Galop", "Il passe à toute vitesse, lance son lasso autour de la pièce cible et la traîne hors de l'échiquier.", "Entraîné")
                PieceType.BISHOP -> PieceIdentity("Le chasseur de primes", "Mercenaire impitoyable vêtu d'un long poncho", "Fast-Draw", "Il dégaine son revolver en un éclair, tire deux balles précises et rengaine dans un nuage de fumée.", "Abattu")
                PieceType.QUEEN -> PieceIdentity("La desperado", "Comtesse du crime aux deux revolvers en or", "Pluie de Dynamite", "Elle déclenche une fusillade acrobatique provoquant l'explosion de dynamites cachées sous la cible.", "Brûlé")
                PieceType.KING -> PieceIdentity("Le shérif", "Garant de la loi portant l'étoile dorée", "Coup de Crosse", "Il avance d'un pas lourd, sommation à la main, et neutralise la pièce adverse d'un coup de crosse.", "Écrasé")
            }
            UniverseId.GRECE -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Hoplite spartiate", "Soldat en armure de bronze avec lance et aspis", "Phalange Perforante", "Il se protège derrière son bouclier rond et assène un coup de dory perforant.", "Tranché")
                PieceType.ROOK -> PieceIdentity("Le colosse", "Statue vivante en pierre / gardien Minotaure", "Séisme Titanesque", "Il frappe le sol à deux mains et crée une onde de choc qui brise la pièce ennemie en mille morceaux.", "Écrasé")
                PieceType.KNIGHT -> PieceIdentity("Pégase thessalien", "Guerrier chevauchant un destrier ailé", "Piqué Céleste", "Il prend son envol, survole le plateau et s'abat en piqué avec une énergie céleste.", "Écrasé")
                PieceType.BISHOP -> PieceIdentity("L'oracle d'Apollon", "Mystique en toge blanche manipulant le feu divin", "Colonne Solaire", "Il lève son bâton sacré et invoque une colonne de flammes mystiques qui consume la cible.", "Brûlé")
                PieceType.QUEEN -> PieceIdentity("Athéna", "Déesse de la guerre armée de l'Égide", "Éclat de l'Égide", "Son bouclier pétrifie la pièce adverse façon Méduse avant de la briser d'un revers de lance.", "Écrasé")
                PieceType.KING -> PieceIdentity("Zeus", "Souverain de l'Olympe trônant avec autorité", "Foudre Divine", "Il lève le bras et catapulte un éclair pur sur la pièce cible pour la réduire en cendres.", "Brûlé")
            }
            UniverseId.JAPON -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Ashigaru", "Paysan-soldat armé d'une longue lance (yari)", "Botte de Yari", "Il exécute une botte rapide en piquant à répétition avec sa lance jusqu'à détruire la pièce.", "Tranché")
                PieceType.ROOK -> PieceIdentity("Daimyo en armure", "Guerrier colossal portant une armure dorée", "Frappe de Kanabo", "Il abat un grand marteau de guerre kanabo sur le sol, écrasant la pièce adverse.", "Écrasé")
                PieceType.KNIGHT -> PieceIdentity("Le ninja", "Assassin de l'ombre vêtu de noir", "Fumigène & Kunai", "Il lance une bombe fumigène, disparaît puis réapparaît dans le dos de la cible pour l'éliminer au kunai.", "Tranché")
                PieceType.BISHOP -> PieceIdentity("Le moine sohei", "Maître spirituel aux talismans ofuda", "Feu des Ofuda", "Il lance une salve de talismans enchantés qui désintègrent la cible dans un feu spirituel bleu.", "Brûlé")
                PieceType.QUEEN -> PieceIdentity("L'onna-musha", "Guerrière légendaire armée d'une naginata", "Danse Mortelle", "Elle enchaîne une danse de coupes circulaires ultra-rapides, tranchant l'air et la cible.", "Tranché")
                PieceType.KING -> PieceIdentity("Le shogun", "Chef suprême au masque de démon (menpo)", "Iaijutsu Sacré", "Il dégaine son katana sacré et tranche l'espace d'un coup net : la cible se fend en deux.", "Tranché")
            }
            UniverseId.BASKET -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Le défenseur", "Rookie en maillot — rôle de défenseur", "Vol de Balle (Steal)", "Il se plante devant la cible, vole la balle et la pièce sort du terrain.", "Entraîné")
                PieceType.ROOK -> PieceIdentity("Le pivot", "Géant sous le panier — rôle de pivot", "Contre (Block)", "Il bloque le tir d'une claque monumentale qui envoie la pièce dans les gradins.", "Écrasé")
                PieceType.KNIGHT -> PieceIdentity("L'ailier", "Ailier explosif en attaque", "Dunk Aérien", "Il prend son élan en diagonale et réalise un dunk en l'air : le panier tremble et la cible est écrasée.", "Écrasé")
                PieceType.BISHOP -> PieceIdentity("Le tireur", "Tireur d'élite à longue distance", "Tir à 3 Points", "Le ballon file en diagonale à travers la pièce adverse et le filet claque.", "Abattu")
                PieceType.QUEEN -> PieceIdentity("La meneuse", "Meneuse de jeu virtuose", "Alley-Oop Smash", "Crossover, passe aveugle puis alley-oop : elle s'élance et réduit la cible en poussière d'un smash.", "Écrasé")
                PieceType.KING -> PieceIdentity("La légende (MVP)", "Star absolue de l'équipe", "Buzzer-Beater", "Un tir de mi-terrain au buzzer avec ralenti, tableau d'affichage et feux d'artifice.", "Brûlé")
            }
            UniverseId.FUTURISTE -> when (pieceType) {
                PieceType.PAWN -> PieceIdentity("Le drone soldat", "Petit drone armé autonome", "Rafale de Pulsar", "Il tire une rafale de pulsar en essaim avec ses voisins.", "Abattu")
                PieceType.ROOK -> PieceIdentity("La forteresse orbitale", "Tourelle lourde de défense", "Canon Orbital", "Elle tire un canon laser depuis l'orbite : le faisceau tombe en colonne sur la cible.", "Brûlé")
                PieceType.KNIGHT -> PieceIdentity("Le cyborg-motard", "Cyborg sur moto antigravité", "Traînée de Néon", "Il fonce et traverse la cible dans une traînée de néon à haute vélocité.", "Tranché")
                PieceType.BISHOP -> PieceIdentity("Le sabre laser", "Guerrier au sabre laser (choix de Laurence)", "Arc Lumineux", "Il frappe en un arc lumineux : la pièce adverse se fend en deux avec une lueur résiduelle.", "Tranché")
                PieceType.QUEEN -> PieceIdentity("L'androïde IA", "Intelligence artificielle à forme d'androïde", "Portail Quantique", "Elle ouvre un portail dimensionnel, aspire la cible et la renvoie en fragments de données.", "Entraîné")
                PieceType.KING -> PieceIdentity("Le noyau quantique", "Cœur de l'armada futuriste", "Singularité", "Une singularité (trou noir) engloutit la pièce adverse.", "Entraîné")
            }
        }
    }
}
