        }

        // Delete Category Confirmation Dialog
        if (uiState.categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteCategoryDialog() },
                title = { Text("Xóa danh mục?") },
                text = { 
                    Text("Bạn có chắc muốn xóa danh mục \"${uiState.categoryToDelete!!.name}\"?\n\nLưu ý: Các món ăn trong danh mục này sẽ không bị xóa.") 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            uiState.categoryToDelete!!.id?.let { id ->
                                viewModel.deleteCategory(id)
                            }
                            viewModel.dismissDeleteCategoryDialog()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteCategoryDialog() }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
