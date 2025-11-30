package com.olcayaras.lib.definitions

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.olcayaras.lib.currentFrame
import com.olcayaras.lib.ofFrames
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlin.time.Duration

@Serializable(with = SequenceDefinitionSerializer::class)
data class SequenceDefinition(
    override val from: Duration,
    override val to: Duration,
    val enter: EnterTransition = fadeIn(),
    val exit: ExitTransition = fadeOut(),
    val content: @Composable SequenceAnimationScope.() -> Unit,
    val tag: Int? = null,
) : VideoComponent

object SequenceDefinitionSerializer : KSerializer<SequenceDefinition> {
    override val descriptor = buildClassSerialDescriptor("SequenceDefinition") {
        element<Duration>("from")
        element<Duration>("to")
    }

    override fun serialize(encoder: Encoder, value: SequenceDefinition) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Duration.serializer(), value.from)
            encodeSerializableElement(descriptor, 1, Duration.serializer(), value.to)
        }
    }

    override fun deserialize(decoder: Decoder): SequenceDefinition {
        throw UnsupportedOperationException("Deserialization is not supported")
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
interface SequenceAnimationScope : SharedTransitionScope {
    val animatedVisibility: AnimatedVisibilityScope
    val sharedTransition: SharedTransitionScope

    @Composable
    fun Modifier.sharedElement(key: String): Modifier {
        return sharedElement(
            sharedTransition.rememberSharedContentState(key),
            animatedVisibility,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SequencesAsVideo(sequences: List<SequenceDefinition>) {
    val currentFrame = currentFrame()
    val activeSequence = sequences.filter { sequence ->
        currentFrame in sequence.from.ofFrames..sequence.to.ofFrames
    }
    SharedTransitionLayout {
        AnimatedContent(
            targetState = activeSequence,
            transitionSpec = {
                val enterTransition = targetState
                    .firstNotNullOfOrNull { sequence -> sequence.enter.takeUnless { it == EnterTransition.None } }
                    ?: EnterTransition.None

                val exitTransition = initialState
                    .firstNotNullOfOrNull { sequence -> sequence.exit.takeUnless { it == ExitTransition.None } }
                    ?: ExitTransition.None

                enterTransition togetherWith exitTransition
            },
        ) { sequence ->
            val sharedTransition = this@SharedTransitionLayout
            val animatedVisibility = this@AnimatedContent

            val scope = remember {
                object : SequenceAnimationScope, SharedTransitionScope by sharedTransition {
                    override val animatedVisibility: AnimatedVisibilityScope = animatedVisibility
                    override val sharedTransition: SharedTransitionScope = sharedTransition
                }
            }

            sequence.forEach {
                it.content(scope)
            }
        }
    }
}
