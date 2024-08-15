package felis.micromixin

import felis.LoaderPluginEntrypoint
import felis.ModLoader
import felis.meta.ModMetadataExtended
import felis.transformer.TransformingClassLoader
import net.peanuuutz.tomlkt.asTomlLiteral
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stianloader.micromixin.transform.api.MixinConfig
import org.stianloader.micromixin.transform.api.MixinLoggingFacade
import org.stianloader.micromixin.transform.api.MixinTransformer
import org.stianloader.micromixin.transform.api.supertypes.ClassWrapperPool

object MicroMixinLoaderPlugin : LoaderPluginEntrypoint {
    data class MixinMetadata(val path: String)

    @Suppress("MemberVisibilityCanBePrivate")
    val ModMetadataExtended.mixin: MixinMetadata?
        get() = this["mixins"]
            ?.asTomlLiteral()
            ?.toString()
            ?.let { MixinMetadata(it) }

    private val logger = LoggerFactory.getLogger("MicroMixin")
    private val transformer: MixinTransformer<TransformingClassLoader> =
        MixinTransformer(TransformingClassLoader::unmodifiedClassNode, ClassWrapperPool()).also {
            it.logger = MMLogger(this.logger)
            it.setMergeClassFileVersions(false)
        }

    override fun onLoaderInit() {
        this.logger.info("Initializing Micromixin")
        var configs = 0
        ModLoader.discoverer.mods.mapNotNull { it.metadata.mixin }.map(MixinMetadata::path).forEach { path ->
            ModLoader.classLoader.getResourceAsStream(path)?.use { it.readAllBytes() }?.let {
                transformer.addMixin(ModLoader.classLoader, MixinConfig.fromString(String(it)))
                configs++
            }
        }

        if (configs > 0) this.logger.info("Micromixin successfully initialized with $configs configuration${if (configs > 1) "s" else ""}")

        ModLoader.transformer.registerTransformation { container ->
            if (transformer.isMixinTarget(container.internalName)) {
                container.node(transformer::transform)
            } else {
                container
            }
        }
    }

    class MMLogger(private val logger: Logger) : MixinLoggingFacade {
        override fun debug(clazz: Class<*>?, message: String?, vararg args: Any?) = logger.debug(message, args)
        override fun error(clazz: Class<*>?, message: String?, vararg args: Any?) = logger.error(message, args)
        override fun info(clazz: Class<*>?, message: String?, vararg args: Any?) = logger.info(message, args)
        override fun warn(clazz: Class<*>?, message: String?, vararg args: Any?) = logger.warn(message, args)
    }
}

