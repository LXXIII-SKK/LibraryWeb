export type RouteState =
  | { name: "home" }
  | { name: "books" }
  | { name: "upcoming" }
  | { name: "account" }
  | { name: "admin" }
  | { name: "book"; bookId: number };

export function resolveRoute(pathname: string): RouteState {
  const bookMatch = pathname.match(/^\/books\/(\d+)$/);
  if (bookMatch) {
    return { name: "book", bookId: Number(bookMatch[1]) };
  }

  if (pathname === "/books") {
    return { name: "books" };
  }

  if (pathname === "/upcoming") {
    return { name: "upcoming" };
  }

  if (pathname === "/me") {
    return { name: "account" };
  }

  if (pathname === "/admin") {
    return { name: "admin" };
  }

  return { name: "home" };
}
