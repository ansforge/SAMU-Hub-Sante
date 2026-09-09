import { ExternalLinkIcon } from "lucide-react";

interface SourceLink {
  href: string;
}
const SourceLink = ({ href }: SourceLink) => (
  <a
    target="_blank"
    className="flex items-center text-sm underline text-primary"
    href={href}
  >
    voir la source <ExternalLinkIcon className="h-3 w-3 ml-1" />
  </a>
);

export default SourceLink;
