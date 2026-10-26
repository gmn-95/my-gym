package com.gmn95.mygym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.ui.theme.MyGymTheme

@Composable
fun MyGymApp() {
    MyGymTheme {
        HomeScreen()
    }
}

@Composable
private fun HomeScreen() {
    Scaffold { innerPadding ->
        HomeContent(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun HomeContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_description),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.stage_label),
            modifier = Modifier.padding(top = 24.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MyGymTheme {
        HomeContent(contentPadding = PaddingValues())
    }
}
