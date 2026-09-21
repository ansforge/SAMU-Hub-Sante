import { ExternalLinkIcon } from "lucide-react";

interface SourceLink {
  href: string;
}
const SourceLink = ({ href }: SourceLink) => (
  <a target="_blank" className="flex items-center text-primary" href={href}>
    <ExternalLinkIcon className="h-4 w-4 ml-1" />
  </a>
);

export default SourceLink;
