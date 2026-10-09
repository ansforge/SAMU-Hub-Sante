import type { ReactNode } from "react";
import { ExternalLinkIcon } from "lucide-react";
import { cn } from "@/lib/utils";

export function ExternalLink({
  href,
  children,
  className,
}: {
  href: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={cn(
        "inline-flex items-center gap-1 text-sm text-primary underline underline-offset-2 hover:text-primary/80",
        className,
      )}
    >
      {children}
      <ExternalLinkIcon aria-hidden className="size-3.5" />
      <span className="sr-only">(nouvelle fenêtre)</span>
    </a>
  );
}
