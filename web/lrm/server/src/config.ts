type VhostClientMap = Record<string, string[]>;

export class Config {
  private port: number;
  private adminPassword: string;
  private hubUrl: string;
  private lrmCertPassphrase: string;
  private hubSanteExchange: string;
  private vhostClientMap: VhostClientMap;

  constructor() {
    this.port = this.extractNumericEnvVar('PORT', 8081);
    this.adminPassword = this.extractEnvVar('ADMIN_PASSWORD');
    this.hubUrl = this.extractEnvVar('HUB_URL');
    this.lrmCertPassphrase = this.extractEnvVar('LRM_PASSPHRASE');
    this.hubSanteExchange = 'hubsante';
    this.vhostClientMap = JSON.parse(this.extractEnvVar('VHOST_CLIENT_MAP'));
  }

  private extractNumericEnvVar(key: string, defaultValue?: number) {
    const value = this.extractEnvVar(key, defaultValue?.toString());
    if (isNaN(Number(value))) {
      throw new Error(`The environment variable "${key}" is not a valid number: ${process.env[key]}`);
    }
    return Number(value);
  }

  private extractEnvVar(key: string, defaultValue?: string) {
    if (process.env[key]) {
      return process.env[key];
    }
    if (defaultValue) {
      return defaultValue;
    }
    throw new Error(
      `The following environment variable is missing: ${key}. In Kubernetes, this might be caused by a missing ConfigMap or Secret.`,
    );
  }

  public getPort() {
    return this.port;
  }

  public getAdminPassword() {
    return this.adminPassword;
  }

  public getHubUrl() {
    return this.hubUrl;
  }

  public getLrmCertPassphrase() {
    return this.lrmCertPassphrase;
  }

  public getHubSanteExchange() {
    return this.hubSanteExchange;
  }

  public getVhostClientMap() {
    return this.vhostClientMap;
  }

  public toString() {
    return `Configuration:\n  - urlPort: ${this.getPort()}\n  - hubUrl: ${this.getHubUrl()}\n  - hubSanteExchange: ${this.getHubSanteExchange()}`;
  }
}
