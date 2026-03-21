type BookCoverArtProps = {
  title: string;
  coverImageUrl: string | null;
  className?: string;
};

function initials(title: string) {
  return title
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export function BookCoverArt({ title, coverImageUrl, className = "" }: BookCoverArtProps) {
  return (
    <div className={`book-cover ${className}`.trim()}>
      {coverImageUrl ? (
        <img src={coverImageUrl} alt={`Cover of ${title}`} loading="lazy" />
      ) : (
        <div className="book-cover-placeholder" aria-hidden="true">
          <span>{initials(title)}</span>
        </div>
      )}
    </div>
  );
}
