package org.wordpress.android.ui.accounts.login.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.color
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
            onClick = onClick,
            enabled = enabled,
            elevation = ButtonDefaults.elevation(2.dp),
            colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    backgroundColor = colorResource(id = color.blue_50),
                    disabledBackgroundColor = MaterialTheme.colors.background,
            ),
            modifier = modifier
                    .fillMaxWidth()
                    .padding(
                            vertical = Margin.Small.value,
                            horizontal = Margin.ExtraExtraMediumLarge.value,
                    )
    ) {
        Text(text = text)
    }
}
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewPrimaryButton() {
    AppTheme {
        PrimaryButton("Button", onClick = {})
    }
}
