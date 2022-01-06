package com.seiko.tl.parser

import com.seiko.tl.parser.ValueType.BOOLEAN
import com.seiko.tl.parser.ValueType.CLASS
import com.seiko.tl.parser.ValueType.INT
import com.seiko.tl.parser.ValueType.LONG
import com.seiko.tl.parser.ValueType.STRING
import com.seiko.tl.parser.ValueType.VECTOR
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GramTLParserTest {
    @Test
    fun `simple_01`() {
        val input = "help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", INT),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "help.SensitiveWordList" to setOf("help.getSensitiveWords"),
            ),
        )
    }

    @Test
    fun `vector_int`() {
        val input = "help.getSensitiveWords#519536bc hash:Vector<int> = help.SensitiveWordList;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", VECTOR(INT)),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "help.SensitiveWordList" to setOf("help.getSensitiveWords"),
            ),
        )
    }

    @Test
    fun `vector_vector_int`() {
        val input = "help.getSensitiveWords#519536bc hash:Vector<Vector<int>> = help.SensitiveWordList;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", VECTOR(VECTOR(INT))),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "help.SensitiveWordList" to setOf("help.getSensitiveWords"),
            ),
        )
    }

    @Test
    fun `flags_01`() {
        val input = "help.sensitiveWords#8ee0b6db flags:# codes:string = help.SensitiveWords;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.sensitiveWords"),
                ConstructorToken("8ee0b6db"),
                FlagsToken(""),
                ValueToken("codes", STRING),
                ReturnToken(CLASS("help.SensitiveWords")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "help.SensitiveWords" to setOf("help.sensitiveWords"),
            ),
        )
    }

    @Test
    fun `flags_02`() {
        val input =
            "enterprise.deleteMessages#1e4a1320 flags:# revoke:flags.0?true id:Vector<int> user_id:long = enterprise.AffectedMessages;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("enterprise.deleteMessages"),
                ConstructorToken("1e4a1320"),
                FlagsToken(""),
                // ValueToken("revoke", CLASS("flags.0?true")),
                FlagsValueToken("revoke", BOOLEAN(true), 0),
                ValueToken("id", VECTOR(INT)),
                ValueToken("user_id", LONG),
                ReturnToken(CLASS("enterprise.AffectedMessages")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "enterprise.AffectedMessages" to setOf("enterprise.deleteMessages"),
            ),
        )
    }

    @Test
    fun `multi_lines_01`() {
        val input =
            "help.sensitiveWords#8ee0b6db flags:# codes:Vector<string> = help.SensitiveWords;\n" +
                "help.sensitiveWordListNotModified#4c5d13a2 = help.SensitiveWordList;\n" +
                "help.sensitiveWordList#104e83a1 sensitiveWords:help.SensitiveWords hash:int = help.SensitiveWordList;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.sensitiveWords"),
                ConstructorToken("8ee0b6db"),
                FlagsToken(""),
                ValueToken("codes", VECTOR(STRING)),
                ReturnToken(CLASS("help.SensitiveWords")),
                //
                ClassToken(name = "help.sensitiveWordListNotModified"),
                ConstructorToken("4c5d13a2"),
                ReturnToken(CLASS("help.SensitiveWordList")),
                //
                ClassToken(name = "help.sensitiveWordList"),
                ConstructorToken("104e83a1"),
                ValueToken("sensitiveWords", CLASS("help.SensitiveWords")),
                ValueToken("hash", INT),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
        assertEquals(
            tokenResult.childClassListMap,
            mapOf(
                "help.SensitiveWords" to setOf("help.sensitiveWords"),
                "help.SensitiveWordList" to setOf("help.sensitiveWordListNotModified", "help.sensitiveWordList"),
            ),
        )
    }

    @Test
    fun `multi_lines_02`() {
        val input =
            "help.systemSettingNew#466027a flags:# user_start_chat_right:int " +
                "registration_mode:int frequent_login_lock_number:int " +
                "clear_chat_history_switch:int user_create_group_right:int " +
                "start_chat_in_group_right:int send_link_in_group_right:int " +
                "registry_interval_same_ip:int registry_nums_same_ip:int " +
                "add_default_friends_switch:int add_default_groups_switch:int " +
                "account_registration_method:Vector<int> = help.SystemSettingNew;\n" +
                "help.enterpriseSystemSettingNewNotModified#e3f85cd0 = help.EnterpriseSystemSettingNew;\n" +
                "help.enterpriseSystemSettingNew#9b03ffb3 " +
                "settings:help.SystemSettingNew hash:int = help.EnterpriseSystemSettingNew;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken(name = "help.systemSettingNew"),
                ConstructorToken(constructor = "466027a"),
                FlagsToken(value = ""),
                ValueToken(name = "user_start_chat_right", type = INT),
                ValueToken(name = "registration_mode", type = INT),
                ValueToken(name = "frequent_login_lock_number", type = INT),
                ValueToken(name = "clear_chat_history_switch", type = INT),
                ValueToken(name = "user_create_group_right", type = INT),
                ValueToken(name = "start_chat_in_group_right", type = INT),
                ValueToken(name = "send_link_in_group_right", type = INT),
                ValueToken(name = "registry_interval_same_ip", type = INT),
                ValueToken(name = "registry_nums_same_ip", type = INT),
                ValueToken(name = "add_default_friends_switch", type = INT),
                ValueToken(name = "add_default_groups_switch", type = INT),
                ValueToken(name = "account_registration_method", type = VECTOR(type = INT)),
                ReturnToken(type = CLASS(name = "help.SystemSettingNew")),
                //
                ClassToken(name = "help.enterpriseSystemSettingNewNotModified"),
                ConstructorToken(constructor = "e3f85cd0"),
                ReturnToken(type = CLASS(name = "help.EnterpriseSystemSettingNew")),
                //
                ClassToken(name = "help.enterpriseSystemSettingNew"),
                ConstructorToken(constructor = "9b03ffb3"),
                ValueToken(name = "settings", type = CLASS(name = "help.SystemSettingNew")),
                ValueToken(name = "hash", type = INT),
                ReturnToken(type = CLASS(name = "help.EnterpriseSystemSettingNew")),
            ),
        )
    }

    @Test
    fun `okio_simple_01`() {
        val input = "help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;"
        val source = Buffer().apply {
            write(input.encodeUtf8())
        }
        val tokenResult = GramTLParser.parseTokens(source)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", INT),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
    }

    @Test
    fun `ignore_01`() {
        val input = "// help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;\n" +
            "help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;" +
            "// help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;\n" +
            "help.getSensitiveWords#519536bc hash:int = help.SensitiveWordList;"
        val tokenResult = GramTLParser.parseTokens(input)
        assertContentEquals(
            tokenResult.tokens,
            listOf(
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", INT),
                ReturnToken(CLASS("help.SensitiveWordList")),
                //
                ClassToken("help.getSensitiveWords"),
                ConstructorToken("519536bc"),
                ValueToken("hash", INT),
                ReturnToken(CLASS("help.SensitiveWordList")),
            ),
        )
    }
}
