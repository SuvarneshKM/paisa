package com.example.paisa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.model.BankPresets
import com.example.paisa.model.BankTheme
import com.example.paisa.model.BankThemeRegistry
import com.example.paisa.model.CardPatternType
import com.example.paisa.model.ChipType
import com.example.paisa.ui.theme.PaisaTheme
import com.example.paisa.util.CurrencyFormatter

/**
 * Standard ID-1 payment card aspect ratio (85.60mm / 53.98mm = 1.58577)
 */
const val BANK_CARD_ASPECT_RATIO = 1.586f

/**
 * Realistic, premium bank account card composable.
 * Renders realistic layered gradients, subtle specular sheen, micro-patterns,
 * beveled perimeter highlights, metallic EMV chip, contactless symbol,
 * and embossed monospace card typography.
 */
@Composable
fun RealisticBankCard(
    bankName: String,
    accountNickname: String,
    lastFourDigits: String,
    balancePaise: Long,
    themeId: String? = null,
    accountType: String = "SAVINGS",
    isPrimary: Boolean = false,
    isReconciled: Boolean = false,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onReconcileClick: (() -> Unit)? = null,
    showActions: Boolean = false,
    modifier: Modifier = Modifier
) {
    val theme: BankTheme = remember(themeId) {
        BankThemeRegistry.getTheme(themeId)
    }

    val cardShape = remember { RoundedCornerShape(18.dp) }

    // Outer physical card container
    Box(
        modifier = modifier
            .aspectRatio(BANK_CARD_ASPECT_RATIO)
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(cardShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else if (onEditClick != null && !showActions) Modifier.clickable { onEditClick() }
                else Modifier
            )
    ) {
        // Layer 1 & 2: Background Canvas with Layered Gradients, Patterns, and Reflection
        CardSurfaceCanvas(
            theme = theme,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 3: Card Content & Typography
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP ROW: Bank Name & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank Name with Jewel Accent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(theme.metallicAccentColor)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = bankName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            fontSize = 12.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Badges (Primary / Type / Reconciled)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReconciled) {
                        CardMicroBadge(
                            text = "RECONCILED",
                            textColor = Color(0xFFE2E8F0),
                            borderColor = Color.White.copy(alpha = 0.3f),
                            backgroundColor = Color.White.copy(alpha = 0.12f)
                        )
                    }

                    if (isPrimary) {
                        CardMicroBadge(
                            text = "PRIMARY",
                            textColor = Color(0xFFFDE68A),
                            borderColor = Color(0xFFFDE68A).copy(alpha = 0.45f),
                            backgroundColor = Color(0xFFFDE68A).copy(alpha = 0.15f)
                        )
                    }

                    CardMicroBadge(
                        text = accountType.uppercase(),
                        textColor = Color.White,
                        borderColor = Color.White.copy(alpha = 0.25f),
                        backgroundColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            // MIDDLE ROW: Metallic EMV Chip & Contactless Wave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetallicChip(
                    chipType = theme.chipType,
                    modifier = Modifier.size(width = 36.dp, height = 27.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                ContactlessWave(
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(width = 16.dp, height = 20.dp)
                )
            }

            // MASKED CARD NUMBER ROW: Embossed Monospace Style
            val formattedDigits = remember(lastFourDigits) {
                val clean = lastFourDigits.filter { it.isDigit() }.takeLast(4)
                val pad = clean.padStart(4, '•')
                "••••   ••••   ••••   $pad"
            }

            Text(
                text = formattedDigits,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 2.2.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 2.5f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // BOTTOM ROW: Nickname & Available Balance + Optional Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Account Nickname
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "ACCOUNT NICKNAME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.4f),
                                offset = Offset(0f, 1f),
                                blurRadius = 1.5f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = accountNickname.ifBlank { "Account" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Balance & Actions
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AVAILABLE BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.4f),
                                offset = Offset(0f, 1f),
                                blurRadius = 1.5f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = CurrencyFormatter.formatPaise(balancePaise),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 2.5f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1
                    )

                    // Optional Action Buttons for Accounts Screen
                    if (showActions && (onEditClick != null || onReconcileClick != null)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (onReconcileClick != null) {
                                GlassActionButton(
                                    text = "Reconcile",
                                    onClick = onReconcileClick
                                )
                            }
                            if (onEditClick != null) {
                                GlassActionButton(
                                    text = "Edit",
                                    onClick = onEditClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overload of RealisticBankCard accepting AccountWithBalance.
 */
@Composable
fun RealisticBankCard(
    accountWithBalance: AccountWithBalance,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onReconcileClick: (() -> Unit)? = null,
    showActions: Boolean = false,
    modifier: Modifier = Modifier
) {
    val account = accountWithBalance.account
    RealisticBankCard(
        bankName = account.bankName,
        accountNickname = account.nickname,
        lastFourDigits = account.lastFourDigits,
        balancePaise = accountWithBalance.calculatedBalancePaise,
        themeId = account.resolvedThemeId,
        accountType = account.accountType,
        isPrimary = account.isPrimary,
        isReconciled = account.reconciledBalancePaise != null,
        onClick = onClick,
        onEditClick = onEditClick,
        onReconcileClick = onReconcileClick,
        showActions = showActions,
        modifier = modifier
    )
}

/**
 * Background Canvas rendering:
 * 1. Base Multi-stop Diagonal Gradient
 * 2. Subtle Bank-specific Micro-pattern (Guilloche / Ripples / Grid / Chevrons)
 * 3. Diagonal Specular Lamination Sheen
 * 4. Beveled Inner Border Highlight
 */
@Composable
private fun CardSurfaceCanvas(
    theme: BankTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Base Layered Gradient
        val gradientBrush = Brush.linearGradient(
            colors = theme.cardGradient,
            start = Offset(0f, 0f),
            end = Offset(width, height)
        )
        drawRect(brush = gradientBrush)

        // 2. Bank-Specific Geometric Micro-pattern (low opacity watermark effect)
        drawBankPattern(theme.patternType, theme.metallicAccentColor, width, height)

        // 3. Diagonal Specular Sheen (Lamination Reflection)
        val sheenBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.02f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            ),
            start = Offset(width * 0.15f, 0f),
            end = Offset(width * 0.85f, height)
        )
        drawRect(brush = sheenBrush)

        // 4. Beveled Edge Highlight & Shadow (authentic 3D plastic / metal rim)
        val bevelBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.35f)
            ),
            start = Offset(0f, 0f),
            end = Offset(width, height)
        )
        drawRoundRect(
            brush = bevelBrush,
            size = size,
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Draws subtle, high-security geometric micro-patterns based on card theme.
 */
private fun DrawScope.drawBankPattern(
    patternType: CardPatternType,
    accentColor: Color,
    width: Float,
    height: Float
) {
    val patternColor = accentColor.copy(alpha = 0.05f)
    val strokeWidth = 1.dp.toPx()

    when (patternType) {
        CardPatternType.WAVES -> {
            // SBI / Canara concentric circular ripple watermark in bottom-right
            val center = Offset(width * 0.88f, height * 0.82f)
            val maxRadius = width * 0.65f
            val step = 16.dp.toPx()
            var r = 16.dp.toPx()
            while (r < maxRadius) {
                drawCircle(
                    color = patternColor,
                    radius = r,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                r += step
            }
        }
        CardPatternType.GRID -> {
            // HDFC / IDFC modern high-tech isometric micro-grid
            val spacing = 22.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = patternColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = strokeWidth
                )
                x += spacing
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = patternColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = strokeWidth
                )
                y += spacing
            }
        }
        CardPatternType.CHEVRON -> {
            // Axis angled luxury chevrons
            val step = 28.dp.toPx()
            var offsetX = width * 0.3f
            while (offsetX < width + height) {
                val path = Path().apply {
                    moveTo(offsetX, 0f)
                    lineTo(offsetX - height * 0.5f, height * 0.5f)
                    lineTo(offsetX, height)
                }
                drawPath(path = path, color = patternColor, style = Stroke(width = strokeWidth))
                offsetX += step
            }
        }
        CardPatternType.SUN_RAYS -> {
            // ICICI radiating warm sun arcs
            val center = Offset(width * 0.15f, height * 0.95f)
            val maxRadius = width * 0.9f
            val step = 18.dp.toPx()
            var r = 18.dp.toPx()
            while (r < maxRadius) {
                drawCircle(
                    color = patternColor,
                    radius = r,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                r += step
            }
        }
        CardPatternType.PINSTRIPES -> {
            // Kotak 45-degree dynamic speed pinstripes
            val spacing = 18.dp.toPx()
            var d = -height
            while (d < width + height) {
                drawLine(
                    color = patternColor,
                    start = Offset(d, 0f),
                    end = Offset(d + height, height),
                    strokeWidth = strokeWidth
                )
                d += spacing
            }
        }
        CardPatternType.CONCENTRIC -> {
            // Federal / South Indian Bank concentric center watermark
            val center = Offset(width * 0.75f, height * 0.45f)
            val maxRadius = width * 0.5f
            val step = 16.dp.toPx()
            var r = 16.dp.toPx()
            while (r < maxRadius) {
                drawCircle(
                    color = patternColor,
                    radius = r,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                r += step
            }
        }
        CardPatternType.MINIMAL -> {
            // Subtle radial highlight vignette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(width * 0.8f, height * 0.2f),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = Offset(width * 0.8f, height * 0.2f)
            )
        }
    }
}

/**
 * Decorative Metallic EMV Smart Card Chip.
 * Rendered using Canvas with metallic brush gradients and authentic etched contact pattern.
 */
@Composable
fun MetallicChip(
    chipType: ChipType,
    modifier: Modifier = Modifier
) {
    val goldGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFD4AF37),
                Color(0xFFF9E79F),
                Color(0xFFB7950B),
                Color(0xFFE5C158)
            )
        )
    }

    val silverGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFCBD5E1),
                Color(0xFFF8FAFC),
                Color(0xFF94A3B8),
                Color(0xFFE2E8F0)
            )
        )
    }

    val baseGradient = if (chipType == ChipType.GOLD) goldGradient else silverGradient
    val strokeColor = if (chipType == ChipType.GOLD) Color(0xFF7A6211) else Color(0xFF64748B)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        // Chip Base Body
        drawRoundRect(
            brush = baseGradient,
            size = size,
            cornerRadius = cornerRadius
        )

        // Chip Outer Bevel Stroke
        drawRoundRect(
            color = strokeColor.copy(alpha = 0.6f),
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = 0.8.dp.toPx())
        )

        // Microchip Contact Etchings (hairline contacts)
        val etchColor = strokeColor.copy(alpha = 0.7f)
        val etchWidth = 0.8.dp.toPx()

        // Horizontal dividing line
        drawLine(
            color = etchColor,
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = etchWidth
        )

        // Upper vertical dividing line
        drawLine(
            color = etchColor,
            start = Offset(w * 0.35f, 0f),
            end = Offset(w * 0.35f, h * 0.5f),
            strokeWidth = etchWidth
        )
        drawLine(
            color = etchColor,
            start = Offset(w * 0.65f, 0f),
            end = Offset(w * 0.65f, h * 0.5f),
            strokeWidth = etchWidth
        )

        // Lower vertical dividing line
        drawLine(
            color = etchColor,
            start = Offset(w * 0.35f, h * 0.5f),
            end = Offset(w * 0.35f, h),
            strokeWidth = etchWidth
        )
        drawLine(
            color = etchColor,
            start = Offset(w * 0.65f, h * 0.5f),
            end = Offset(w * 0.65f, h),
            strokeWidth = etchWidth
        )

        // Center Micro-Core Contact
        drawRoundRect(
            color = etchColor,
            topLeft = Offset(w * 0.28f, h * 0.32f),
            size = Size(w * 0.44f, h * 0.36f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = etchWidth)
        )
    }
}

/**
 * Universal Contactless Payment Wave symbol.
 * 4 concentric arcs radiating to the right.
 */
@Composable
fun ContactlessWave(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.5f
        val strokeWidth = 1.3.dp.toPx()

        // 3 concentric wave arcs radiating to the right
        val radii = listOf(w * 0.3f, w * 0.65f, w * 1.0f)
        for (radius in radii) {
            val left = -radius * 0.4f
            val top = centerY - radius
            drawArc(
                color = tint,
                startAngle = -42f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Sleek micro-badge with glass effect.
 */
@Composable
private fun CardMicroBadge(
    text: String,
    textColor: Color,
    borderColor: Color,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = textColor
        )
    }
}

/**
 * Frosted glass action button (Edit / Reconcile) on the card surface.
 */
@Composable
private fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(0.75.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

// =============================================================================
// COMPOSE PREVIEWS (SBI, HDFC, Axis, ICICI, Kotak, Custom Themes)
// =============================================================================

@Preview(name = "HDFC Bank Card", showBackground = true)
@Composable
fun RealisticBankCardPreview_HDFC() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "HDFC Bank",
                accountNickname = "Primary Salary",
                lastFourDigits = "4821",
                balancePaise = 14250000L,
                themeId = "HDFC",
                accountType = "SALARY",
                isPrimary = true
            )
        }
    }
}

@Preview(name = "SBI Card", showBackground = true)
@Composable
fun RealisticBankCardPreview_SBI() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "State Bank of India",
                accountNickname = "Savings Account",
                lastFourDigits = "6231",
                balancePaise = 8520000L,
                themeId = "SBI",
                accountType = "SAVINGS",
                isReconciled = true
            )
        }
    }
}

@Preview(name = "Axis Bank Card", showBackground = true)
@Composable
fun RealisticBankCardPreview_Axis() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "Axis Bank",
                accountNickname = "Emergency Fund",
                lastFourDigits = "9012",
                balancePaise = 25000000L,
                themeId = "AXIS",
                accountType = "CURRENT"
            )
        }
    }
}

@Preview(name = "ICICI Bank Card", showBackground = true)
@Composable
fun RealisticBankCardPreview_ICICI() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "ICICI Bank",
                accountNickname = "Shopping & UPI",
                lastFourDigits = "1145",
                balancePaise = 3140000L,
                themeId = "ICICI",
                accountType = "SAVINGS"
            )
        }
    }
}

@Preview(name = "Kotak Card", showBackground = true)
@Composable
fun RealisticBankCardPreview_Kotak() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "Kotak Mahindra Bank",
                accountNickname = "Daily Expenses",
                lastFourDigits = "7788",
                balancePaise = 960000L,
                themeId = "KOTAK",
                accountType = "SAVINGS"
            )
        }
    }
}

@Preview(name = "Custom Theme - Emerald Velvet", showBackground = true)
@Composable
fun RealisticBankCardPreview_CustomEmerald() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "Saraswat Bank",
                accountNickname = "Fixed Deposits",
                lastFourDigits = "3321",
                balancePaise = 50000000L,
                themeId = "THEME_EMERALD",
                accountType = "SAVINGS"
            )
        }
    }
}

@Preview(name = "Custom Theme - Obsidian Slate", showBackground = true)
@Composable
fun RealisticBankCardPreview_CustomObsidian() {
    PaisaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RealisticBankCard(
                bankName = "Private Reserve",
                accountNickname = "Investments",
                lastFourDigits = "0001",
                balancePaise = 125000000L,
                themeId = "THEME_MONOCHROME",
                accountType = "CURRENT",
                isPrimary = true
            )
        }
    }
}
