package codewhale.doortreeandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

enum class PolicyDocument(
    val titleKey: String,
    val bodyKey: String,
    val icon: String,
    val url: String
) {
    Eula(
        titleKey = "policy.eula.title",
        bodyKey = "eula_text",
        icon = "lock.shield",
        url = "https://example.com/doortree/eula"
    ),
    CodeOfConduct(
        titleKey = "policy.code_of_conduct.title",
        bodyKey = "policy.code_of_conduct.body",
        icon = "checklist",
        url = "https://example.com/doortree/code-of-conduct"
    ),
    PrivacyPolicy(
        titleKey = "policy.privacy_policy.title",
        bodyKey = "policy.privacy_policy.body",
        icon = "person.crop.rectangle.stack.fill",
        url = "https://doortree.co/privacy-policy"
    ),
    TermsOfUse(
        titleKey = "policy.terms_of_use.title",
        bodyKey = "policy.terms_of_use.body",
        icon = "doc.text",
        url = "https://doortree.co/terms-and-conditions"
    )
}

@Composable
fun PolicyDocumentSheetView(
    document: PolicyDocument,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(DoorTreeTheme.backgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = systemIcon(document.icon),
                contentDescription = null,
                tint = DoorTreeTheme.textSecondary,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = L(document.titleKey),
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(DoorTreeTheme.backgroundSecondary, CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = systemIcon("xmark"),
                    contentDescription = null,
                    tint = DoorTreeTheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = L(document.bodyKey),
            color = DoorTreeTheme.textSecondary,
            lineHeight = 22.sp,
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 22.dp)
                .padding(18.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.size(12.dp))
    }
}
