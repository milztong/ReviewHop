import { SystemStatusCard } from "@/features/system/components/system-status-card";
import { auth, signIn } from "@/auth";

const stack = ["Java 21", "Spring Boot 3", "PostgreSQL", "Next.js", "Testcontainers"];

export default async function Home() {
  const session = await auth();

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="ReviewHop Startseite">
          <span className="brand__mark" aria-hidden="true">RH</span>
          <span>ReviewHop</span>
        </a>
        <span className="phase-badge">Phase 7</span>
      </header>

      <section className="hero" id="top">
        <div className="hero__copy">
          <p className="eyebrow">Spaced repetition for solved problems</p>
          <h1>Gelöst heißt noch nicht behalten.</h1>
          <p className="hero__lead">
            ReviewHop verwandelt bereits bearbeitete Lernaufgaben in gezielte Wiederholungen.
            Der produktionsnahe Lernfluss verbindet sichere Anmeldung, Java-Backend,
            PostgreSQL und nachvollziehbare Observability.
          </p>
          <ul className="stack-list" aria-label="Technologien im Walking Skeleton">
            {stack.map((technology) => <li key={technology}>{technology}</li>)}
          </ul>
          <div className="hero__actions">
            {session ? (
              <a className="primary-button" href="/dashboard">Zum Dashboard</a>
            ) : (
              <form action={async () => {
                "use server";
                await signIn("keycloak", { redirectTo: "/dashboard" });
              }}>
                <button className="primary-button" type="submit">Mit OIDC anmelden</button>
              </form>
            )}
          </div>
        </div>

        <SystemStatusCard />
      </section>

      <section className="principles" aria-labelledby="principles-heading">
        <p className="eyebrow">Built deliberately</p>
        <h2 id="principles-heading">Ein Fundament, das spätere Features nicht bekämpfen müssen.</h2>
        <div className="principle-grid">
          <article>
            <span>01</span>
            <h3>Fachlich modular</h3>
            <p>Package by Feature, geprüfte Modulgrenzen und ein frameworkfreier Domainkern.</p>
          </article>
          <article>
            <span>02</span>
            <h3>Von Beginn an testbar</h3>
            <p>Unit-, Architektur- und PostgreSQL-Integrationstests laufen in derselben Pipeline.</p>
          </article>
          <article>
            <span>03</span>
            <h3>Cloud-ready</h3>
            <p>Dieselben Container sind für lokale Entwicklung und die Hetzner-Demo vorbereitet.</p>
          </article>
        </div>
      </section>
    </main>
  );
}
