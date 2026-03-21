export type BookFormErrors = {
  title?: string;
  author?: string;
  category?: string;
  isbn?: string;
};

export function validateBookForm(input: {
  title: string;
  author: string;
  category: string;
  isbn: string;
  coverImageUrl?: string | null;
}): BookFormErrors {
  const errors: BookFormErrors = {};

  if (!input.title.trim()) {
    errors.title = "Title is required.";
  } else if (input.title.trim().length > 255) {
    errors.title = "Title must be 255 characters or fewer.";
  }

  if (!input.author.trim()) {
    errors.author = "Author is required.";
  } else if (input.author.trim().length > 255) {
    errors.author = "Author must be 255 characters or fewer.";
  }

  if (input.category.trim().length > 100) {
    errors.category = "Category must be 100 characters or fewer.";
  }

  if (input.isbn.trim().length > 50) {
    errors.isbn = "ISBN must be 50 characters or fewer.";
  }

  return errors;
}
