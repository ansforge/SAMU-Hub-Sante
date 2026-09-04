import { useCallback, useState } from "react";

export function useCopyToClipBoard() {
  const [isCopied, setIsCopied] = useState(false);

  const handleCopyToClipBoard = useCallback(async (content: string) => {
    setIsCopied(true);
    await navigator.clipboard.writeText(content);
    setTimeout(() => setIsCopied(false), 1000);
  }, []);

  return {
    handleCopyToClipBoard,
    isCopied,
  };
}
