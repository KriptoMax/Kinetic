package kz.kripto.studycompose1.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.ui.theme.StudyCompose1Theme

@Composable
fun KineticAddFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.ic_add)
) {
    FloatingActionButton(
        modifier = modifier.size(65.dp),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(45.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun KineticReturnFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.ic_arrowleft)
) {
    FloatingActionButton(
        modifier = modifier.size(65.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(14.dp)
            ),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(showBackground = true,
    device = "spec:width=412dp,height=915dp,dpi=450",
    showSystemUi = true)
@Composable
fun ReturnFABPreview() {
    StudyCompose1Theme {
        Surface(
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            KineticReturnFAB(onClick = {})
        }
    }
}
