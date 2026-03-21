import { AdminConsole } from "./AdminConsole";
import type { AdminConsoleProps } from "../view-models";

export function AdminPage(props: AdminConsoleProps) {
  return (
    <section className="page-stack">
      <section className="surface admin-hero">
        <div className="section-heading">
          <div>
            <p className="section-label">Operations Workspace</p>
            <h2>Task-based operations with a sidebar, dashboard, and focused work surfaces.</h2>
          </div>
        </div>
        <p className="hero-text books-hero-text">
          Each section is isolated into its own task surface: dashboard, catalog, inventory,
          upcoming books, notifications, circulation, access control, branches, locations, and
          policies.
        </p>
      </section>

      <AdminConsole {...props} />
    </section>
  );
}
