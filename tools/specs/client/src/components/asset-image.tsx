import { cn } from "@/lib/utils";

export function AssetImage({
  name,
  alt,
  className,
}: {
  name: string;
  alt: string;
  className?: string;
}) {
  return (
    <img
      src={`${import.meta.env.BASE_URL}${name}`}
      alt={alt}
      className={cn("w-auto max-w-none shrink-0", className)}
    />
  );
}
