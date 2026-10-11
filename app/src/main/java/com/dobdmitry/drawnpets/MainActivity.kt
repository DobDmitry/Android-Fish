package com.dobdmitry.drawnpets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.album.AlbumScreen
import com.dobdmitry.drawnpets.ui.care.CareScreen
import com.dobdmitry.drawnpets.ui.common.DrawnPetsTheme
import com.dobdmitry.drawnpets.ui.create.CreateScreen
import com.dobdmitry.drawnpets.ui.games.GamesMenuScreen
import com.dobdmitry.drawnpets.ui.games.MiniGame
import com.dobdmitry.drawnpets.ui.games.PlayScreen
import com.dobdmitry.drawnpets.ui.pond.PondScreen
import com.dobdmitry.drawnpets.ui.shop.ShopScreen
import com.dobdmitry.drawnpets.ui.world.WorldScreen

sealed interface Screen {
    data object World : Screen
    data object Pond : Screen
    data object Create : Screen
    data object Shop : Screen
    data object Album : Screen
    data class Care(val id: String) : Screen
    data class Games(val creatureId: String?) : Screen
    data class Play(val game: MiniGame, val creatureId: String?) : Screen
}

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrawnPetsTheme {
                App(vm)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.saveNow()
    }
}

@Composable
fun App(vm: GameViewModel) {
    val stack = remember { mutableStateListOf<Screen>(Screen.World) }
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    /** Переход «наверх» — к экрану, но без накопления одинаковых экранов в истории. */
    fun open(s: Screen) {
        val existing = stack.indexOfLast { it == s }
        if (existing >= 0) {
            while (stack.size > existing + 1) stack.removeAt(stack.lastIndex)
        } else push(s)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screens",
    ) { screen ->
        when (screen) {
            Screen.World -> WorldScreen(
                vm = vm,
                onCreate = { open(Screen.Create) },
                onPond = { open(Screen.Pond) },
                onGames = { open(Screen.Games(null)) },
                onShop = { open(Screen.Shop) },
                onAlbum = { open(Screen.Album) },
                onCare = { open(Screen.Care(it)) },
            )
            Screen.Pond -> PondScreen(vm, onBack = ::pop, onCare = { open(Screen.Care(it)) })
            Screen.Create -> CreateScreen(vm, onBack = ::pop) { _, habitat ->
                pop()
                if (habitat == Habitat.POND) open(Screen.Pond) else open(Screen.World)
            }
            Screen.Shop -> ShopScreen(vm, onBack = ::pop)
            Screen.Album -> AlbumScreen(vm, onBack = ::pop, onCare = { open(Screen.Care(it)) }, onCreate = { open(Screen.Create) })
            is Screen.Care -> CareScreen(vm, screen.id, onBack = ::pop, onPlay = { open(Screen.Games(it)) })
            is Screen.Games -> GamesMenuScreen(vm, screen.creatureId, onBack = ::pop) { game, id ->
                push(Screen.Play(game, id))
            }
            is Screen.Play -> PlayScreen(vm, screen.game, screen.creatureId, onExit = ::pop)
        }
    }
}
