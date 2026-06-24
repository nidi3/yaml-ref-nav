package guru.nidi.yamlrefnav

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class RefResolutionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    fun testInFileRefResolvesToDefinitionKey() {
        myFixture.configureByFile("inFileRef.yaml")
        assertEquals("ArtTeam", resolvedKey()?.keyText)
    }

    fun testNestedPointerWalksTheMappingTree() {
        myFixture.configureByFile("nestedRef.yaml")
        assertEquals("Inner", resolvedKey()?.keyText)
    }

    fun testSelfReferenceByFileNameResolves() {
        myFixture.configureByFile("selfname/data.yaml")
        assertEquals("ArtTeam", resolvedKey()?.keyText)
    }

    fun testRelativeCrossDirRefResolves() {
        // First path holds the caret; the rest are copied into the fixture VFS.
        myFixture.configureByFiles("xref/sub/child.yaml", "xref/data.yaml")
        assertEquals("ArtTeam", resolvedKey()?.keyText)
    }

    fun testRefWithoutFragmentResolvesToTheFile() {
        myFixture.configureByFiles("wholefile/child.yaml", "wholefile/target.yaml")
        val resolved = referenceAtCaret()?.resolve()
        assertInstanceOf(resolved, YAMLFile::class.java)
        assertEquals("target.yaml", (resolved as YAMLFile).name)
    }

    fun testUnresolvableFragmentReturnsNull() {
        myFixture.configureByFile("brokenRef.yaml")
        assertNull(referenceAtCaret()?.resolve())
    }

    fun testNonRefScalarHasNoReference() {
        myFixture.configureByFile("plainScalar.yaml")
        assertNull(referenceAtCaret())
    }

    private fun referenceAtCaret() = myFixture.getReferenceAtCaretPosition()

    private fun resolvedKey(): YAMLKeyValue? {
        val resolved: PsiElement = referenceAtCaret()?.resolve() ?: return null
        return resolved as? YAMLKeyValue ?: resolved.parent as? YAMLKeyValue
    }
}
