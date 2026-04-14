package top.yudoge.phoneclaw.emu.domain

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yudoge.phoneclaw.emu.domain.objects.AppInfo
import top.yudoge.phoneclaw.emu.domain.objects.UITree
import top.yudoge.phoneclaw.emu.domain.objects.UIWindow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class LuaValueConverterTest {

    private val converter = LuaValueConverter()

    @Test
    fun `converts primitive values`() {
        assertEquals("42", converter.toLuaValue(42).tojstring())
        assertEquals("hello", converter.toLuaValue("hello").tojstring())
        assertTrue(converter.toLuaValue(true).toboolean())
        assertTrue(converter.toLuaValue(null).isnil())
    }

    @Test
    fun `converts list with lua 1-based indexing`() {
        val table = converter.listToTable(listOf("a", "b", "c"))

        assertEquals(3, table.length())
        assertEquals("a", table.get(1).tojstring())
        assertEquals("b", table.get(2).tojstring())
        assertEquals("c", table.get(3).tojstring())
    }

    @Test
    fun `converts app info into stable keys`() {
        val table = converter.appInfoToTable(AppInfo(packageName = "com.demo.app", appName = "Demo"))

        assertEquals("com.demo.app", table.get("packageName").tojstring())
        assertEquals("Demo", table.get("appName").tojstring())
    }

    @Test
    fun `converts ui tree recursively with bounds and children`() {
        val child = UITree(text = "child", bounds = Rect(10, 20, 30, 40))
        val root = UITree(
            id = "root-id",
            className = "android.widget.TextView",
            text = "root",
            children = listOf(child)
        )
        val window = UIWindow(packageName = "com.demo", root = root, matchedNodes = listOf(root))

        val table = converter.uiWindowToTable(window)
        val rootTable = table.get("root")
        val childTable = rootTable.get("children").get(1)

        assertEquals("com.demo", table.get("packageName").tojstring())
        assertEquals("root-id", rootTable.get("id").tojstring())
        assertEquals("child", childTable.get("text").tojstring())
        assertEquals(10, childTable.get("bounds").get("left").toint())
    }

    @Test
    fun `optional fields map to nil-compatible semantics`() {
        val tree = UITree(text = null, hintText = null, bounds = null)
        val table = converter.uiTreeToTable(tree)

        assertTrue(table.get("text").isnil())
        assertTrue(table.get("hintText").isnil())
        assertTrue(table.get("bounds").isnil())
        assertFalse(table.get("children").isnil())
    }
}
