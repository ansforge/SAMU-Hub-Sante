import { useCopyToClipBoard } from "@/hooks/use-copy";
import { Button } from "./ui/button";
import { Check, Copy } from "lucide-react";

type CopyButtonProps = {
  content: string;
};

export const CopyButton = ({ content }: CopyButtonProps) => {
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
      className="hidden group-hover:flex border-0"
    >
      {isCopied ? <Check /> : <Copy className="h-4 w-4" />}
    </Button>
  );
};
