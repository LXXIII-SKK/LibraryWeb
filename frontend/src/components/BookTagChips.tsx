function formatTag(tag: string) {
  return tag
    .split("-")
    .map((part) => part.slice(0, 1).toUpperCase() + part.slice(1))
    .join(" ");
}

type BookTagChipsProps = {
  tags: string[];
  className?: string;
};

export function BookTagChips({ tags, className }: BookTagChipsProps) {
  if (tags.length === 0) {
    return null;
  }

  return (
    <div className={className ?? "tag-strip"}>
      {tags.map((tag) => (
        <span key={tag} className="tag-chip">
          {formatTag(tag)}
        </span>
      ))}
    </div>
  );
}
