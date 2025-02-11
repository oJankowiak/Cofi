@file:OptIn(ExperimentalMaterial3Api::class)

package com.omelan.cofi.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.omelan.cofi.R
import com.omelan.cofi.components.*
import com.omelan.cofi.model.Recipe
import com.omelan.cofi.model.RecipeIcon
import com.omelan.cofi.model.Step
import com.omelan.cofi.ui.Spacing
import com.omelan.cofi.ui.createTextSelectionColors
import com.omelan.cofi.ui.modal
import com.omelan.cofi.ui.shapes
import com.omelan.cofi.utils.getDefaultPadding
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class, ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun RecipeEdit(
    saveRecipe: (Recipe, List<Step>) -> Unit,
    goBack: () -> Unit = {},
    stepsToEdit: List<Step> = listOf(),
    recipeToEdit: Recipe = Recipe(name = "", description = "", recipeIcon = RecipeIcon.Grinder),
    deleteRecipe: () -> Unit = {},
    cloneRecipe: (Recipe, List<Step>) -> Unit = { _, _ -> },
    isEditing: Boolean = false,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(
        DpSize(1920.dp, 1080.dp)
    ),
) {
    var showDeleteModal by remember { mutableStateOf(false) }
    var showCloneModal by remember { mutableStateOf(false) }
    var showSaveModal by remember { mutableStateOf(false) }
    var pickedIcon by remember(recipeToEdit) { mutableStateOf(recipeToEdit.recipeIcon) }
    var name by remember(recipeToEdit) { mutableStateOf(recipeToEdit.name) }
    var description by remember(recipeToEdit) { mutableStateOf(recipeToEdit.description) }
    var steps by remember(stepsToEdit) { mutableStateOf(stepsToEdit) }
    var stepWithOpenEditor by remember { mutableStateOf<Step?>(null) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val appBarBehavior = createAppBarBehavior()
    val lazyListState = rememberLazyListState()
    val textSelectionColors = MaterialTheme.createTextSelectionColors()

    val canSave = name.isNotBlank() && steps.isNotEmpty()
    val configuration = LocalConfiguration.current

    val isPhoneLayout by remember(
        windowSizeClass.widthSizeClass,
        configuration.screenHeightDp,
        configuration.screenWidthDp
    ) {
        derivedStateOf {
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
                (configuration.screenHeightDp > configuration.screenWidthDp)
        }
    }

    val safeGoBack: () -> Unit = {
        if (steps !== stepsToEdit ||
            name != recipeToEdit.name ||
            description != recipeToEdit.description ||
            pickedIcon != recipeToEdit.recipeIcon
        ) {
            showSaveModal = true
        } else {
            goBack()
        }
    }

    BackHandler {
        if (stepWithOpenEditor != null) {
            stepWithOpenEditor = null
            return@BackHandler
        }
        safeGoBack()
    }

    val onSave: () -> Unit = {
        saveRecipe(
            recipeToEdit.copy(name = name, description = description, recipeIcon = pickedIcon),
            steps.mapIndexed { index, step -> step.copy(orderInRecipe = index) }
        )
    }

    fun pickIcon(icon: RecipeIcon) {
        coroutineScope.launch {
            bottomSheetScaffoldState.bottomSheetState.collapse()
            pickedIcon = icon
        }
    }

    val renderNameAndDescriptionEdit: LazyListScope.() -> Unit = {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                                bottomSheetScaffoldState.bottomSheetState.collapse()
                            } else {
                                bottomSheetScaffoldState.bottomSheetState.expand()
                            }
                            keyboardController?.hide()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = pickedIcon.icon),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_edit_name"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences),
                    label = { Text(stringResource(id = R.string.recipe_edit_name)) },
                )
            }
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.big)
                    .testTag("recipe_edit_description"),
                keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences),
                label = { Text(stringResource(id = R.string.recipe_edit_description)) },
            )
        }
    }

    val renderSteps: LazyListScope.() -> Unit = {
        itemsIndexed(
            steps,
            { _, step -> if (step.id == 0) step.hashCode() else step.id }
        ) { index, step ->
            AnimatedVisibility(
                modifier = Modifier.animateItemPlacement(),
                visible = stepWithOpenEditor == step,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                StepAddCard(
                    stepToEdit = step,
                    save = { stepToSave ->
                        steps = if (stepToSave == null) {
                            steps.minus(step)
                        } else {
                            steps.mapIndexed { mapIndex, step ->
                                if (index == mapIndex) {
                                    stepToSave
                                } else {
                                    step
                                }
                            }
                        }
                        stepWithOpenEditor = null
                    },
                    isFirst = index == 0,
                    isLast = index == steps.size - 1,
                    onPositionChange = { change ->
                        steps = steps.toMutableList().apply {
                            add(index + change, removeAt(index))
                        }
                    },
                    orderInRecipe = index,
                    recipeId = recipeToEdit.id,
                )
            }
            AnimatedVisibility(
                visible = stepWithOpenEditor != step,
                enter = expandVertically(),
                modifier = Modifier.animateItemPlacement(),
                exit = shrinkVertically(),
            ) {
                StepListItem(
                    step = step,
                    stepProgress = StepProgress.Upcoming,
                    onClick = { stepWithOpenEditor = it }
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = stepWithOpenEditor == null,
                enter = expandVertically(),
                modifier = Modifier.animateItemPlacement(),
                exit = shrinkVertically(),
            ) {
                StepAddCard(
                    onTypeSelect = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(
                                lazyListState.layoutInfo.totalItemsCount - 1
                            )
                        }
                    },
                    modifier = Modifier.animateItemPlacement(),
                    save = { stepToSave ->
                        if (stepToSave != null) {
                            steps = steps.toMutableList().apply { add(stepToSave) }
                        }
                    },
                    orderInRecipe = steps.size,
                    recipeId = recipeToEdit.id,
                )
            }
        }
    }
    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        modifier = Modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
        sheetPeekHeight = 0.dp,
        sheetElevation = 30.dp,
        sheetShape = shapes.modal,
        sheetBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        sheetContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(getDefaultPadding(skipNavigationBarPadding = true))
            ) {
                RecipeIcon.values().map {
                    IconButton(
                        onClick = { pickIcon(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = Spacing.big)
                    ) {
                        Icon(
                            painter = painterResource(id = it.icon),
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = it.name,
                        )
                    }
                }
            }
        },
        topBar = {
            PiPAwareAppBar(
                navigationIcon = {
                    IconButton(onClick = safeGoBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showCloneModal = true }) {
                            Icon(
                                painterResource(id = R.drawable.ic_copy),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { showDeleteModal = true }) {
                            Icon(
                                painterResource(id = R.drawable.ic_delete),
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.testTag("recipe_edit_save"),
                        onClick = onSave,
                        enabled = canSave,
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_save),
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Text(
                        text = if (isEditing) {
                            stringResource(id = R.string.recipe_edit_title)
                        } else {
                            stringResource(id = R.string.recipe_add_new_title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                scrollBehavior = appBarBehavior,
            )
        }
    ) {
        CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
            BoxWithConstraints {
                if (isPhoneLayout) {
                    PhoneLayout(
                        it,
                        maxHeight,
                        lazyListState,
                        renderNameAndDescriptionEdit,
                        renderSteps
                    )
                } else {
                    TabletLayout(
                        it,
                        maxHeight,
                        lazyListState,
                        renderNameAndDescriptionEdit,
                        renderSteps
                    )
                }
            }
        }

        if (showDeleteModal && isEditing) {
            DeleteDialog(onConfirm = deleteRecipe, onDismiss = { showDeleteModal = false })
        }
        if (showSaveModal) {
            SaveDialog(
                canSave = canSave,
                onSave = onSave,
                onDiscard = goBack,
                onDismiss = { showSaveModal = false }
            )
        }
        if (showCloneModal) {
            CloneDialog(onConfirm = {
                cloneRecipe(
                    recipeToEdit.copy(
                        name = name,
                        description = description,
                        recipeIcon = pickedIcon,
                    ),
                    steps.mapIndexed { index, step -> step.copy(orderInRecipe = index) }
                )
            }, onDismiss = { showCloneModal = false })
        }
    }
}

@Composable
fun PhoneLayout(
    paddingValues: PaddingValues,
    maxHeight: Dp,
    lazyListState: LazyListState,
    renderNameAndDescriptionEdit: LazyListScope.() -> Unit,
    renderSteps: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        state = lazyListState,
        contentPadding = getDefaultPadding(
            paddingValues = paddingValues,
            additionalBottomPadding = maxHeight / 2
        ),
    ) {
        renderNameAndDescriptionEdit()
        renderSteps()
    }
}

@Composable
fun TabletLayout(
    paddingValues: PaddingValues,
    maxHeight: Dp,
    lazyListState: LazyListState,
    renderNameAndDescriptionEdit: LazyListScope.() -> Unit,
    renderSteps: LazyListScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(getDefaultPadding(paddingValues)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            contentPadding = PaddingValues(bottom = maxHeight / 2)
        ) {
            renderNameAndDescriptionEdit()
        }
        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = maxHeight / 2)
        ) {
            renderSteps()
        }
    }
}

@Composable
fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.button_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.button_cancel))
            }
        },
        icon = { Icon(painterResource(id = R.drawable.ic_delete), null) },
        title = {
            Text(text = stringResource(id = R.string.recipe_delete_title))
        },
        text = {
            Text(text = stringResource(id = R.string.recipe_delete_text))
        },
    )
}

@Composable
fun SaveDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    canSave: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = if (canSave) onSave else onDismiss
            ) {
                Text(
                    stringResource(
                        if (canSave) {
                            R.string.step_add_save
                        } else {
                            R.string.button_continue_editing
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(text = stringResource(id = R.string.button_discard))
            }
        },
        icon = { Icon(painterResource(id = R.drawable.ic_save), null) },
        title = {
            Text(text = stringResource(id = R.string.recipe_unsaved_title))
        },
        text = {
            Text(text = stringResource(id = R.string.recipe_unsaved_text))
        },
    )
}

@Composable
fun CloneDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.button_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.button_cancel))
            }
        },
        icon = { Icon(painterResource(id = R.drawable.ic_copy), null) },
        title = {
            Text(text = stringResource(id = R.string.recipe_clone_title))
        },
        text = {
            Text(text = stringResource(id = R.string.recipe_clone_text))
        },
    )
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Preview
@Composable
fun RecipeEditPreview() {
    RecipeEdit(saveRecipe = { _, _ -> })
}