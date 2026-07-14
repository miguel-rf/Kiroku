package com.kiroku.app.core.common

/**
 * Produces readable text from the small Markdown subset returned by the API.
 * URLs are not opened or interpreted here; link labels remain visible.
 */
fun markdownToPlainText(source: String): String = source
    .replace(MARKDOWN_LINK) { match -> match.groupValues[1] }
    .replace("**", "")
    .replace("__", "")
    .replace(HEADING_PREFIX, "")
    .replace(TRAILING_SPACES_BEFORE_NEWLINE, "\n")
    .replace(EXCESS_BLANK_LINES, "\n\n")
    .trim()

private val MARKDOWN_LINK = Regex("""\[([^\]]+)]\((?:[^()]|\([^)]*\))+\)""")
private val HEADING_PREFIX = Regex("""(?m)^\s{0,3}#{1,6}\s+""")
private val TRAILING_SPACES_BEFORE_NEWLINE = Regex("""[ \t]+\n""")
private val EXCESS_BLANK_LINES = Regex("""\n{3,}""")
