import { useId, useState } from "react";
import type { DueReview, ReviewRating } from "@/features/learning/model";

const ratings: ReviewRating[] = ["AGAIN", "HARD", "GOOD", "EASY"];

interface RecallCardProps {
  review: DueReview;
  note: string;
  completing: boolean;
  onNoteChange: (note: string) => void;
  onRate: (rating: ReviewRating) => void;
}

export function RecallCard({ review, note, completing, onNoteChange, onRate }: RecallCardProps) {
  const [revealed, setRevealed] = useState(false);
  const titleId = useId();
  const approachId = useId();

  return (
    <article className="review-item" aria-labelledby={titleId}>
      <header>
        <span>{review.difficulty ?? "Ohne Schwierigkeit"}</span>
        <h3 id={titleId}>{review.title}</h3>
      </header>
      {!revealed && (
        <div className="recall-card__content">
          <h4>Aufgabenfrage</h4>
          {review.recallPrompt ? (
            <p>{review.recallPrompt}</p>
          ) : (
            <p>Die Aufgabenstellung findest du über den Link zur Originalaufgabe.</p>
          )}
        </div>
      )}
      <div className="recall-card__actions">
        <button
          className="primary-button"
          type="button"
          aria-expanded={revealed}
          aria-controls={approachId}
          onClick={() => setRevealed(!revealed)}
        >
          {revealed ? "Zurück zur Frage" : "Lösungsansatz aufdecken"}
        </button>
        <a className="secondary-button" href={review.sourceUrl} target="_blank" rel="noreferrer">
          Zur Originalaufgabe
        </a>
      </div>
      <div id={approachId} hidden={!revealed}>
        {revealed && (
          <div className="recall-card__back">
            <div className="recall-card__content">
              <h4>Mein Lösungsansatz</h4>
              <p>{review.approachNotes || "Für diese Karte ist noch kein Lösungsansatz hinterlegt."}</p>
            </div>
            <label className="review-note">
              Optionale Review-Notiz
              <textarea
                maxLength={2000}
                onChange={(event) => onNoteChange(event.target.value)}
                rows={2}
                value={note}
              />
            </label>
            <div className="rating-row" aria-label={`Bewertung für ${review.title}`}>
              {ratings.map((rating) => (
                <button disabled={completing} key={rating} type="button" onClick={() => onRate(rating)}>
                  {rating}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
