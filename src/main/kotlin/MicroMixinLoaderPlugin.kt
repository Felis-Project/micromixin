package felis.micromixin

import felis.LoaderPluginEntrypoint
import felis.ModLoader
import felis.meta.ModMetadataExtended
import felis.transformer.ClassContainer
import felis.transformer.Transformation
import felis.transformer.TransformingClassLoader
import net.peanuuutz.tomlkt.asTomlLiteral
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stianloader.micromixin.transform.api.MixinConfig
import org.stianloader.micromixin.transform.api.MixinLoggingFacade
import org.stianloader.micromixin.transform.api.MixinTransformer
import org.stianloader.micromixin.transform.api.supertypes.ClassWrapper
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
    private val classWrappers = ClassWrapperPool().also {
        it.addProvider { name, pool ->
            // better than parsing a node at runtime using ASMClassWrapperProvider
            ModLoader.classLoader.getClassData(name)
                ?.bytes
                ?.let(::ClassReader)
                ?.run {
                    ClassWrapper(
                        name,
                        superName,
                        interfaces,
                        access and Opcodes.ACC_INTERFACE != 0,
                        pool
                    )
                }
        }
    }
    private val transformer: MixinTransformer<TransformingClassLoader> =
        MixinTransformer<TransformingClassLoader>({ loader, name ->
            loader.getClassData(name)?.node ?: throw ClassNotFoundException("Class $name was not found")
        }, this.classWrappers).also {
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

        if (configs > 0)
            this.logger.info("Micromixin successfully initialized with $configs configuration${if (configs > 1) "s" else ""}")
        ModLoader.transformer.registerTransformation(MicroMixinTransformation)
    }

    class MMClassWriter : ClassWriter(COMPUTE_FRAMES) {
        override fun getCommonSuperClass(type1: String, type2: String): String = transformer.pool.let {
            it.getCommonSuperClass(it.get(type1), it.get(type2)).name
        }
    }

    object MicroMixinTransformation : Transformation {
        override fun transform(container: ClassContainer) {
            if (transformer.isMixinTarget(container.internalName)) {
                transformer.transform(container.node)
                with(MMClassWriter()) {
                    container.node.accept(this)
                    container.newBytes(this.toByteArray())
                }
            }
        }
    }

    class MMLogger(private val logger: Logger) : MixinLoggingFacade {
        override fun debug(p0: Class<*>?, message: String?, vararg args: Any?) =
            logger.debug(message, args)

        override fun error(clazz: Class<*>?, message: String?, vararg args: Any?) =
            logger.error(message, args)

        override fun info(clazz: Class<*>?, message: String?, vararg args: Any?) =
            logger.info(message, args)

        override fun warn(clazz: Class<*>?, message: String?, vararg args: Any?) =
            logger.warn(message, args)
    }
}

