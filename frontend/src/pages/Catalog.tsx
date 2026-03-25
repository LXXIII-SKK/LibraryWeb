import { useBooks, useUpcomingBooks, useDiscovery, useBookFilters } from "../hooks/useQueries";
import { BooksWorkspacePage } from "../components/BooksWorkspacePage";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export function Catalog() {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [tagFilter, setTagFilter] = useState("all");

  const booksQuery = useBooks(query, categoryFilter === "all" ? undefined : categoryFilter, tagFilter === "all" ? undefined : tagFilter);
  const filtersQuery = useBookFilters();
  const upcomingQuery = useUpcomingBooks();
  const discoveryQuery = useDiscovery();

  return (
    <BooksWorkspacePage
      loading={booksQuery.isLoading}
      canBorrow={false} // Would come from auth context
      canReserve={false}
      canManageCatalog={false}
      query={query}
      categoryFilter={categoryFilter}
      tagFilter={tagFilter}
      categories={filtersQuery.data?.categories || []}
      tags={filtersQuery.data?.tags || []}
      books={booksQuery.data || []}
      onQueryChange={setQuery}
      onCategoryChange={setCategoryFilter}
      onTagChange={setTagFilter}
      onBorrow={() => {}}
      onReserve={() => {}}
      onStartEdit={() => {}}
      onOpenBook={(id) => navigate(`/book/${id}`)}
      onNavigateUpcoming={() => navigate("/upcoming")}
    />
  );
}
