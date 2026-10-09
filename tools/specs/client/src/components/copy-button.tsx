import { useCopyToClipBoard } from "@/hooks/use-copy";
import { Button } from "./ui/button";
import { Check, Copy } from "lucide-react";

type CopyButtonProps = {
  content: string;
  label?: string;
};

export const CopyButton = ({ content, label = "Copier" }: CopyButtonProps) => {
  const { handleCopyToClipBoard, isCopied } = useCopyToClipBoard();

  const handleCopy = (e: { stopPropagation: () => void }) => {
    e.stopPropagation();
    handleCopyToClipBoard(content);
  };

  return (
    <Button
      onClick={handleCopy}
      size="icon-xs"
      variant={"ghost"}
      aria-label={label}
      title={label}
      className="border-0 opacity-0 group-hover:opacity-100 focus-visible:opacity-100"
    >
      {isCopied ? <Check /> : <Copy className="h-4 w-4" />}
    </Button>
  );
};
