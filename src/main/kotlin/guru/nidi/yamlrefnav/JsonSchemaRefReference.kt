package guru.nidi.yamlrefnav

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLValue

/**
 * Resolves a JSON-Schema `$ref` written in YAML, e.g.
 *  - `#/$defs/Foo`                                  (in-file, incl. nested `#/$defs/A/B`)
 *  - `data.yaml#/$defs/Foo`                         (sibling or self by file name)
 *  - `../sticky_type/data.yaml#/$defs/StickyType`   (relative path)
 *  - `../shared_models.yaml`                        (whole file, no fragment)
 */
class JsonSchemaRefReference(
    scalar: YAMLScalar,
    private val refText: String,
) : PsiReferenceBase<YAMLScalar>(scalar, true) {

    override fun getRangeInElement(): TextRange {
        // Highlight just the ref value inside the (possibly quoted) scalar token.
        val idx = element.text.indexOf(refText)
        return if (idx >= 0) TextRange(idx, idx + refText.length) else TextRange(0, element.textLength)
    }

    override fun resolve(): PsiElement? {
        val hashIndex = refText.indexOf('#')
        val filePart = if (hashIndex >= 0) refText.substring(0, hashIndex) else refText
        val fragment = if (hashIndex >= 0) refText.substring(hashIndex + 1) else ""

        val targetFile = resolveFile(filePart) ?: return null
        if (fragment.isBlank()) return targetFile

        return resolvePointer(targetFile, fragment)
    }

    private fun resolveFile(filePart: String): YAMLFile? {
        val containingFile = element.containingFile?.originalFile ?: return null
        val currentVf = containingFile.virtualFile ?: return null

        val targetVf = if (filePart.isEmpty()) {
            currentVf
        } else {
            currentVf.parent?.findFileByRelativePath(filePart) ?: return null
        }

        return PsiManager.getInstance(element.project).findFile(targetVf) as? YAMLFile
    }

    /** Walks a JSON Pointer like `/$defs/Foo/bar` down the YAML mapping tree. */
    private fun resolvePointer(file: YAMLFile, fragment: String): PsiElement? {
        val parts = fragment.split('/')
            .filter { it.isNotEmpty() }
            .map(::decodePointerSegment)

        var current: YAMLValue? = file.documents.firstOrNull()?.topLevelValue
        var result: PsiElement? = file
        for (part in parts) {
            val mapping = current as? YAMLMapping ?: return null
            val keyValue = mapping.getKeyValueByKey(part) ?: return null
            result = keyValue.key ?: keyValue
            current = keyValue.value
        }
        return result
    }

    /** JSON Pointer escaping: `~1` -> `/`, `~0` -> `~`. */
    private fun decodePointerSegment(segment: String): String =
        segment.replace("~1", "/").replace("~0", "~")

    override fun getVariants(): Array<Any> = emptyArray()
}
