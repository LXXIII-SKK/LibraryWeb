import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import * as api from "../api";
import { BookFilters, DiscoveryResponse, Book, UpcomingBook, LibraryBranch } from "../types";

export const queryKeys = {
  books: (query: string, category?: string, tag?: string) => ["books", query, category, tag],
  bookFilters: ["bookFilters"],
  discovery: ["discovery"],
  upcoming: ["upcomingBooks"],
  publicBranches: ["publicBranches"],
  profile: ["profile"],
};

export function useBooks(query: string, category?: string, tag?: string) {
  return useQuery({
    queryKey: queryKeys.books(query, category, tag),
    queryFn: () => api.fetchBooks(query, category, tag),
  });
}

export function useBookFilters() {
  return useQuery({
    queryKey: queryKeys.bookFilters,
    queryFn: api.fetchBookFilters,
  });
}

export function useDiscovery() {
  return useQuery({
    queryKey: queryKeys.discovery,
    queryFn: api.fetchDiscovery,
  });
}

export function useUpcomingBooks() {
  return useQuery({
    queryKey: queryKeys.upcoming,
    queryFn: api.fetchUpcomingBooks,
  });
}

export function usePublicBranches() {
  return useQuery({
    queryKey: queryKeys.publicBranches,
    queryFn: api.fetchPublicBranches,
  });
}

export function useProfile(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.profile,
    queryFn: api.fetchProfile,
    enabled,
  });
}
