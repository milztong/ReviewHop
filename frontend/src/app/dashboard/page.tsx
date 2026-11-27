import { auth, signOut } from "@/auth";
import { LearningDashboard } from "@/features/learning/learning-dashboard";
import { redirect } from "next/navigation";

export default async function DashboardPage() {
  const session = await auth();
  if (!session) {
    redirect("/");
  }

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="/dashboard" aria-label="ReviewHop Dashboard">
          <span className="brand__mark" aria-hidden="true">RH</span>
          <span>ReviewHop</span>
        </a>
        <form action={async () => {
          "use server";
          await signOut({ redirectTo: "/" });
        }}>
          <button className="secondary-button" type="submit">Abmelden</button>
        </form>
      </header>
      <section className="dashboard-intro">
        <p className="eyebrow">Phase 5 · Codewars-Integration</p>
        <h1>Was willst du dauerhaft behalten?</h1>
      </section>
      <LearningDashboard />
    </main>
  );
}
