package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import com.adibarra.utils.ADRoman;

public class RomanNumeralGameTest implements FabricGameTest {

    // toRoman basic numerals

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanBasics(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(1).equals("I"), "1 should be I");
        helper.assertTrue(ADRoman.toRoman(4).equals("IV"), "4 should be IV");
        helper.assertTrue(ADRoman.toRoman(9).equals("IX"), "9 should be IX");
        helper.assertTrue(ADRoman.toRoman(14).equals("XIV"), "14 should be XIV");
        helper.complete();
    }

    // toRoman tens/hundreds/thousands

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanTens(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(40).equals("XL"), "40 should be XL");
        helper.assertTrue(ADRoman.toRoman(90).equals("XC"), "90 should be XC");
        helper.assertTrue(ADRoman.toRoman(400).equals("CD"), "400 should be CD");
        helper.assertTrue(ADRoman.toRoman(900).equals("CM"), "900 should be CM");
        helper.assertTrue(ADRoman.toRoman(1000).equals("M"), "1000 should be M");
        helper.complete();
    }

    // toRoman large composites

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanLarge(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(3999).equals("MMMCMXCIX"), "3999 should be MMMCMXCIX");
        helper.assertTrue(ADRoman.toRoman(2024).equals("MMXXIV"), "2024 should be MMXXIV");
        helper.complete();
    }

    // toRoman non-positive

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanNonPositive(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(0).equals(""), "0 should be an empty string");
        helper.assertTrue(ADRoman.toRoman(-5).equals(""), "-5 should be an empty string");
        helper.complete();
    }

    // toRoman upper clamp (pathological input)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanUpperClamp(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(3999).equals("MMMCMXCIX"),
            "3999 stays MMMCMXCIX (clamp boundary, unchanged)");
        helper.assertTrue(ADRoman.toRoman(4000).equals("MMMCMXCIX"), "4000 clamps to MMMCMXCIX");
        helper.assertTrue(ADRoman.toRoman(1_000_000).equals("MMMCMXCIX"), "1,000,000 clamps to MMMCMXCIX");
        helper.assertTrue(ADRoman.toRoman(Integer.MAX_VALUE).equals("MMMCMXCIX"),
            "Integer.MAX_VALUE clamps to MMMCMXCIX");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(1_000_000).equals("MMMCMXCIX"),
            "override(1,000,000) is bounded via the clamp");
        helper.complete();
    }

    // enchantmentLevelOverride gating

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void levelOverrideGating(TestContext helper) {
        helper.assertTrue("0".equals(ADRoman.enchantmentLevelOverride(0)), "level 0 should override to \"0\"");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(1) == null, "level 1 should defer to vanilla (null)");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(5) == null, "level 5 should defer to vanilla (null)");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(10) == null, "level 10 should defer to vanilla (null)");
        helper.assertTrue("XI".equals(ADRoman.enchantmentLevelOverride(11)), "level 11 should override to XI");
        helper.assertTrue("MMMCMXCIX".equals(ADRoman.enchantmentLevelOverride(3999)),
            "level 3999 should override to MMMCMXCIX");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(-5) == null, "level -5 should defer to vanilla (null)");
        helper.complete();
    }

    // levelKeyOverride translation-key gate

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void levelKeyOverrideGate(TestContext helper) {
        // mixin injectors use prefix and parseInt guards
        helper.assertTrue("XI".equals(ADRoman.levelKeyOverride("enchantment.level.11")),
            "enchantment.level.11 should override to XI");
        helper.assertTrue("0".equals(ADRoman.levelKeyOverride("enchantment.level.0")),
            "enchantment.level.0 should override to \"0\"");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.10") == null,
            "enchantment.level.10 should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.") == null,
            "empty suffix should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.max") == null,
            "non-numeric suffix should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.99999999999") == null,
            "int-overflow suffix should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.-5") == null,
            "negative suffix should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("gui.done") == null, "non-level key should defer to vanilla (null)");
        helper.complete();
    }

    // toRoman remaining single digits

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanOneThroughTen(TestContext helper) {
        // complements basic coverage with remaining single digits
        helper.assertTrue(ADRoman.toRoman(2).equals("II"), "2 should be II");
        helper.assertTrue(ADRoman.toRoman(3).equals("III"), "3 should be III");
        helper.assertTrue(ADRoman.toRoman(5).equals("V"), "5 should be V");
        helper.assertTrue(ADRoman.toRoman(6).equals("VI"), "6 should be VI");
        helper.assertTrue(ADRoman.toRoman(7).equals("VII"), "7 should be VII");
        helper.assertTrue(ADRoman.toRoman(8).equals("VIII"), "8 should be VIII");
        helper.assertTrue(ADRoman.toRoman(10).equals("X"), "10 should be X");
        helper.assertTrue(ADRoman.toRoman(11).equals("XI"), "11 should be XI");
        helper.complete();
    }

    // toRoman extreme negative boundaries

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanExtremeNegative(TestContext helper) {
        // non-positive inputs return before sign handling
        // including Integer.MIN_VALUE
        helper.assertTrue(ADRoman.toRoman(-1).equals(""), "-1 should be an empty string");
        helper.assertTrue(ADRoman.toRoman(Integer.MIN_VALUE).equals(""), "Integer.MIN_VALUE should be an empty string");
        helper.complete();
    }

    // toRoman output length stays bounded

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void toRomanBoundedLength(TestContext helper) {
        helper.assertTrue(ADRoman.toRoman(3888).equals("MMMDCCCLXXXVIII"),
            "3888 should be MMMDCCCLXXXVIII (longest standard numeral)");
        helper.assertTrue(ADRoman.toRoman(3888).length() == 15, "3888 numeral should be exactly 15 chars");
        helper.assertTrue(ADRoman.toRoman(Integer.MAX_VALUE).length() <= 15,
            "clamped MAX_VALUE numeral stays within the 15-char ceiling");
        helper.complete();
    }

    // enchantmentLevelOverride int-extreme boundaries

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void levelOverrideExtremes(TestContext helper) {
        // covers sign and overflow boundaries omitted above
        helper.assertTrue(ADRoman.enchantmentLevelOverride(-1) == null, "level -1 should defer to vanilla (null)");
        helper.assertTrue(ADRoman.enchantmentLevelOverride(Integer.MIN_VALUE) == null,
            "level Integer.MIN_VALUE should defer to vanilla (null)");
        helper.assertTrue("MMMCMXCIX".equals(ADRoman.enchantmentLevelOverride(Integer.MAX_VALUE)),
            "level Integer.MAX_VALUE should clamp to MMMCMXCIX");
        helper.complete();
    }

    // levelKeyOverride null and large values

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void levelKeyOverrideNullAndLarge(TestContext helper) {
        // null keys return before startsWith runs
        helper.assertTrue(ADRoman.levelKeyOverride(null) == null,
            "null key should defer to vanilla (null), never throw");
        // large values pass through the complete override chain
        helper.assertTrue("MMMCMXCIX".equals(ADRoman.levelKeyOverride("enchantment.level.3999")),
            "enchantment.level.3999 should override to MMMCMXCIX");
        // Integer.MAX_VALUE also passes through the complete chain
        helper.assertTrue("MMMCMXCIX".equals(ADRoman.levelKeyOverride("enchantment.level.2147483647")),
            "enchantment.level.2147483647 (Integer.MAX_VALUE) should clamp to MMMCMXCIX");
        // negative minimum values parse then defer to vanilla
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.-2147483648") == null,
            "enchantment.level.-2147483648 (Integer.MIN_VALUE) should defer to vanilla (null)");
        helper.complete();
    }

    // levelKeyOverride Integer.parseInt quirks

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void levelKeyOverrideParseQuirks(TestContext helper) {
        // parseInt accepts leading zeros
        helper.assertTrue("XI".equals(ADRoman.levelKeyOverride("enchantment.level.011")),
            "leading-zero suffix 011 parses to 11 -> XI");
        // parseInt accepts leading plus signs
        helper.assertTrue("XI".equals(ADRoman.levelKeyOverride("enchantment.level.+11")),
            "leading-plus suffix +11 parses to 11 -> XI");
        // parseInt treats negative zero as zero
        helper.assertTrue("0".equals(ADRoman.levelKeyOverride("enchantment.level.-0")),
            "negative-zero suffix -0 parses to 0 -> \"0\"");
        // parseInt rejects surrounding or internal whitespace
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level. 11 ") == null,
            "whitespace-padded suffix should defer to vanilla (null)");
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.   ") == null,
            "whitespace-only suffix should defer to vanilla (null)");
        // embedded non-digits return null
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level.1a") == null,
            "embedded-non-digit suffix 1a should defer to vanilla (null)");
        // keys without a trailing dot skip parsing
        helper.assertTrue(ADRoman.levelKeyOverride("enchantment.level") == null,
            "prefix without trailing dot should defer to vanilla (null)");
        // prefix comparison is case-sensitive
        helper.assertTrue(ADRoman.levelKeyOverride("Enchantment.level.11") == null,
            "case-mismatched prefix should defer to vanilla (null)");
        helper.complete();
    }
}
