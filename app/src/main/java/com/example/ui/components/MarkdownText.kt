package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalSanaColors

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String) : MdBlock
    data class Bullet(val marker: String, val text: String) : MdBlock
    data class CodeBlock(val code: String) : MdBlock
}

@Composable
fun SanaMarkdownText(
    markdown: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    Text(
                        text = parseInlineFormatting(block.text, sanaColors.codeBackground.copy(alpha = 0.08f)),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = parseInlineFormatting(block.text, sanaColors.codeBackground.copy(alpha = 0.08f)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                is MdBlock.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = block.marker,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = sanaColors.tealDeep
                        )
                        Text(
                            text = parseInlineFormatting(block.text, sanaColors.codeBackground.copy(alpha = 0.08f)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MdBlock.CodeBlock -> {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(sanaColors.codeBackground)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                ),
                                color = sanaColors.codeText
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(input: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    val lines = input.replace("\r\n", "\n").split("\n")
    var inCode = false
    val codeBuf = StringBuilder()
    val paraBuf = StringBuilder()

    fun flushPara() {
        if (paraBuf.isNotEmpty()) {
            result.add(MdBlock.Paragraph(paraBuf.toString().trim()))
            paraBuf.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            flushPara()
            if (inCode) {
                result.add(MdBlock.CodeBlock(codeBuf.toString().trimEnd()))
                codeBuf.clear()
                inCode = false
            } else {
                inCode = true
            }
            continue
        }

        if (inCode) {
            codeBuf.appendLine(line)
            continue
        }

        when {
            trimmed.isEmpty() -> flushPara()
            trimmed.startsWith("#") -> {
                flushPara()
                result.add(MdBlock.Heading(trimmed.trimStart('#').trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushPara()
                result.add(MdBlock.Bullet("•", trimmed.drop(2).trim()))
            }
            Regex("^[0-9٠-٩]+[.)]\\s+.*").matches(trimmed) -> {
                flushPara()
                val idx = trimmed.indexOfFirst { it == ' ' }
                if (idx > 0) {
                    result.add(MdBlock.Bullet(trimmed.substring(0, idx), trimmed.substring(idx + 1).trim()))
                } else {
                    result.add(MdBlock.Paragraph(trimmed))
                }
            }
            else -> {
                if (paraBuf.isNotEmpty()) paraBuf.append('\n')
                paraBuf.append(trimmed)
            }
        }
    }

    if (inCode && codeBuf.isNotEmpty()) {
        result.add(MdBlock.CodeBlock(codeBuf.toString().trimEnd()))
    }
    flushPara()
    return result
}

private fun parseInlineFormatting(text: String, codeBg: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}
