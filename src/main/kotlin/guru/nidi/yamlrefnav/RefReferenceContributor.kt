package guru.nidi.yamlrefnav

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Registers a [PsiReference] on every YAML scalar that is the value of a `$ref:` key,
 * so Ctrl/Cmd-click navigates to the referenced definition.
 */
class RefReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY
                    val keyValue = scalar.parent as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
                    if (keyValue.keyText != "\$ref") return PsiReference.EMPTY_ARRAY

                    val refText = scalar.textValue
                    if (refText.isBlank()) return PsiReference.EMPTY_ARRAY

                    return arrayOf(JsonSchemaRefReference(scalar, refText))
                }
            },
        )
    }
}
