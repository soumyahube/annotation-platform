# train_model.py
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import accuracy_score, f1_score, classification_report
import json
import sys
import os

def train_model_from_csv(csv_path):
    print("=== DEMARRAGE DE L'ENTRAINEMENT ===")
    print(f"[INFO] Fichier : {csv_path}")

    if not os.path.exists(csv_path):
        print(f"[ERROR] Fichier non trouve !")
        return None

    try:
        df = pd.read_csv(csv_path)
        print(f"[OK] {len(df)} lignes chargees")
    except Exception as e:
        print(f"[ERROR] {e}")
        return None

    if 'texte' not in df.columns or 'classe' not in df.columns:
        print("[ERROR] Colonnes 'texte' et 'classe' requises")
        return None

    df = df.dropna(subset=['texte', 'classe'])
    df['classe'] = df['classe'].str.strip()

    classes = df['classe'].unique()
    print(f"[INFO] Classes : {classes}")

    if len(classes) < 2:
        print("[ERROR] Besoin d'au moins 2 classes")
        return None

    min_class_count = df['classe'].value_counts().min()
    print(f"[INFO] Plus petite classe : {min_class_count} echantillons")

    vectorizer = TfidfVectorizer(max_features=500)
    X = vectorizer.fit_transform(df['texte'])
    y = df['classe']

    if len(df) >= 6 and min_class_count >= 2:
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.3, random_state=42, stratify=y
        )
        # Utiliser .shape[0] pour les matrices sparse
        print(f"[INFO] Entrainement: {X_train.shape[0]} exemples, Test: {X_test.shape[0]} exemples")
    else:
        X_train, X_test, y_train, y_test = X, X, y, y
        print("[INFO] Pas assez de donnees pour le test")

    model = MultinomialNB()
    model.fit(X_train, y_train)
    print("[OK] Modele entraine")

    y_pred = model.predict(X_test)

    if X_test.shape[0] > 0 and len(set(y_test)) > 1:
        accuracy = accuracy_score(y_test, y_pred)
        f1 = f1_score(y_test, y_pred, average='weighted')
        print(f"\n[RESULTAT] Accuracy: {accuracy:.2%}")
        print(f"[RESULTAT] F1-Score: {f1:.2%}")
        print("\n[RAPPORT] Classification:")
        print(classification_report(y_test, y_pred))
    else:
        accuracy = 0.0
        f1 = 0.0
        print("[INFO] Pas assez de donnees pour evaluer le modele")

    import joblib
    joblib.dump(model, 'model_nlp.pkl')
    joblib.dump(vectorizer, 'vectorizer.pkl')
    print("[OK] Modele sauvegarde")

    return {
        "accuracy": round(accuracy * 100, 2),
        "f1Score": round(f1 * 100, 2),
        "samples": len(df),
        "classes": ", ".join(classes)
    }

if __name__ == "__main__":
    csv_path = sys.argv[1] if len(sys.argv) > 1 else "uploaded_dataset.csv"
    result = train_model_from_csv(csv_path)
    if result:
        print(f"\n__METRICS__{json.dumps(result)}")
    else:
        print("\n__METRICS__{}")