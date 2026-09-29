import { SchemaExample } from "@/types";
import { useState } from "react";
import { Button } from "../ui/button";
import { buildGithubSchemaExampleUrl } from "@/lib/utils";
import { githubDomain } from "@/config";

type SchemaExamplesProps = {
  examples: SchemaExample[];
  ref: string;
};

const DEFAULT_DISPLAY_EXAMPLES = 2;

export function SchemaExamples({ examples, ref }: SchemaExamplesProps) {
  const [viewMore, setViewMore] = useState<boolean>(false);
  const exampleList = !viewMore
    ? examples.slice(0, DEFAULT_DISPLAY_EXAMPLES)
    : examples;

  return (
    <div className="flex flex-col items-start mt-3">
      <p className="text-sm">Exemples</p>
      <ul>
        {exampleList.map((ex) => (
          <li key={ex.file}>
            <a
              target="_blank"
              rel="noreferrer"
              className="underline text-primary"
              href={buildGithubSchemaExampleUrl(
                githubDomain,
                `blob/${ref}`,
                ex.file,
              )}
            >
              {[ex.victim, ex.name].filter(Boolean).join(" - ")}
            </a>
          </li>
        ))}
      </ul>
      {examples.length > DEFAULT_DISPLAY_EXAMPLES && (
        <Button
          onClick={() => setViewMore((prev) => !prev)}
          variant={"ghost"}
          size={"xs"}
          className={"border-none h-7 text-xs"}
        >
          {viewMore ? "Voir moins" : "Voir plus"}
        </Button>
      )}
    </div>
  );
}
