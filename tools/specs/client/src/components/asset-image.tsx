export function AssetImage({
  name,
  alt,
  size,
}: {
  name: string;
  alt: string;
  size: number;
}) {
  return (
    <img
      src={`${import.meta.env.BASE_URL}${name}`}
      alt={alt}
      height={size}
    />
  );
}
