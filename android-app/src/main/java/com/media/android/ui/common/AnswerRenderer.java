package com.media.android.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.media.android.R;

/**
 * Renders the structured MEDIA answer markup as readable, hierarchical medical typography.
 *
 * <p>The renderer is deliberately layout-independent: it understands headings, dividers, bullets,
 * evidence lines, generic table rows and inline {@code **emphasis**}, and produces one
 * {@link SpannableStringBuilder}. It never assumes the answer contains every section, so partial
 * records render cleanly. Text selection and copy are inherited from {@code AppCompatTextView}.</p>
 */
public class AnswerRenderer extends androidx.appcompat.widget.AppCompatTextView {

    private int colorHeading;
    private int colorBody;
    private int colorMuted;
    private int colorAccent;
    private int colorDivider;

    public AnswerRenderer(@NonNull Context context) {
        this(context, null);
    }

    public AnswerRenderer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AnswerRenderer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initColors();
        setTextIsSelectable(true);
    }

    private void initColors() {
        colorHeading = ContextCompat.getColor(getContext(), R.color.md_on_background);
        colorBody = ContextCompat.getColor(getContext(), R.color.md_on_surface);
        colorMuted = ContextCompat.getColor(getContext(), R.color.md_on_surface_variant);
        colorAccent = ContextCompat.getColor(getContext(), R.color.md_primary);
        colorDivider = ContextCompat.getColor(getContext(), R.color.md_divider);
    }

    /** Re-resolves theme colours (call after a configuration change if needed). */
    public void refreshColors() {
        initColors();
    }

    /** Parses and displays {@code markup}. */
    public void renderMarkup(@Nullable String markup) {
        if (markup == null || markup.isEmpty()) {
            setText("");
            return;
        }
        setText(buildSpannable(markup));
    }

    private CharSequence buildSpannable(String markup) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = markup.split("\n", -1);
        boolean previousBlank = true;
        boolean inTable = false;

        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();

            if (line.isEmpty()) {
                if (!previousBlank) out.append('\n');
                previousBlank = true;
                inTable = false;
                continue;
            }
            previousBlank = false;

            if (line.equals("---") || line.startsWith("━━")) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
                appendDivider(out);
                out.append('\n');
                inTable = false;
                continue;
            }

            if (line.startsWith("| ")) {
                // Generic table support: the first row of a block acts as the header.
                inTable = !inTable;
                String cells = line.substring(1).trim();
                if (cells.startsWith("---")) continue; // markdown separator row
                String rendered = cells.replace("|", "  ·  ").trim();
                int start = out.length();
                out.append(rendered);
                if (inTable) {
                    out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new ForegroundColorSpan(colorHeading), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    out.setSpan(new ForegroundColorSpan(colorBody), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                out.append('\n');
                continue;
            }

            if (line.startsWith("### ")) {
                appendHeading(out, line.substring(4), 1.06f, colorAccent);
            } else if (line.startsWith("## ")) {
                appendHeading(out, line.substring(3), 1.15f, colorHeading);
            } else if (line.startsWith("# ")) {
                appendHeading(out, line.substring(2), 1.34f, colorHeading);
            } else if (line.startsWith("- ")) {
                appendBullet(out, line.substring(2));
            } else if (line.startsWith("> ")) {
                appendEvidence(out, line.substring(2));
            } else {
                appendParagraph(out, line);
            }
        }
        trimTrailingNewlines(out);
        return out;
    }

    private void appendHeading(SpannableStringBuilder out, String text, float scale, int color) {
        if (out.length() > 1 && out.charAt(out.length() - 1) == '\n') out.append('\n');
        int start = out.length();
        out.append(text.trim());
        int end = out.length();
        out.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new RelativeSizeSpan(scale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private void appendBullet(SpannableStringBuilder out, String text) {
        int start = out.length();
        out.append("\u2022  ");
        int bodyStart = out.length();
        appendInline(out, text);
        int end = out.length();
        out.setSpan(new ForegroundColorSpan(colorAccent), start, bodyStart,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new StyleSpan(Typeface.BOLD), start, bodyStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new LeadingMarginSpan.Standard(0, dp(18)), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private void appendEvidence(SpannableStringBuilder out, String text) {
        int start = out.length();
        out.append("\u25AE  ");
        appendInline(out, text);
        int end = out.length();
        out.setSpan(new ForegroundColorSpan(colorDivider), start, start + 3,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new ForegroundColorSpan(colorMuted), start + 3, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new StyleSpan(Typeface.ITALIC), start + 3, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new LeadingMarginSpan.Standard(0, dp(18)), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private void appendParagraph(SpannableStringBuilder out, String text) {
        appendInline(out, text);
        out.append('\n');
    }

    /** Appends text, converting {@code **x**} into bold spans. */
    private void appendInline(SpannableStringBuilder out, String text) {
        int index = 0;
        while (index < text.length()) {
            int open = text.indexOf("**", index);
            if (open < 0) {
                appendColored(out, text.substring(index));
                break;
            }
            int close = text.indexOf("**", open + 2);
            if (close < 0) {
                appendColored(out, text.substring(index));
                break;
            }
            if (open > index) appendColored(out, text.substring(index, open));
            int start = out.length();
            out.append(text.substring(open + 2, close));
            out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = close + 2;
        }
    }

    private void appendColored(SpannableStringBuilder out, String text) {
        int start = out.length();
        out.append(text);
        out.setSpan(new ForegroundColorSpan(colorBody), start, out.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void appendDivider(SpannableStringBuilder out) {
        int start = out.length();
        StringBuilder line = new StringBuilder(24);
        for (int i = 0; i < 24; i++) line.append('\u2500');
        out.append(line);
        out.setSpan(new ForegroundColorSpan(colorDivider), start, out.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private static void trimTrailingNewlines(SpannableStringBuilder out) {
        while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
            out.delete(out.length() - 1, out.length());
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
