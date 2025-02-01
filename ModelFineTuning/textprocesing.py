import json
import os
from pathlib import Path
import spacy
from collections import Counter
import random

def load_nlp():
    """Load spaCy model for text analysis"""
    try:
        return spacy.load("en_core_web_sm")
    except OSError:
        print("Downloading spaCy model...")
        os.system("python -m spacy download en_core_web_sm")
        return spacy.load("en_core_web_sm")

def analyze_content(text, nlp):
    """Analyzes the content using NLP to understand the main topic and intent."""
    doc = nlp(text)
    
    # Get sentence count
    sentences = list(doc.sents)
    
    analysis = {
        'entities': [(ent.text, ent.label_) for ent in doc.ents],
        'nouns': [token.text for token in doc if token.pos_ in ['NOUN', 'PROPN']],
        'verbs': [token.lemma_ for token in doc if token.pos_ == 'VERB'],
        'adjectives': [token.text for token in doc if token.pos_ == 'ADJ'],
        'sentence_count': len(sentences),
        'is_question': any(sent.text.strip().endswith('?') for sent in sentences),
        'sentence_types': []
    }
    
    # Analyze each sentence type
    for sent in sentences:
        sent_doc = nlp(sent.text)
        # Check for various sentence patterns
        if any(token.dep_ == 'ROOT' and token.pos_ == 'VERB' for token in sent_doc):
            if sent_doc[0].text.lower() in ['how', 'why', 'what', 'when', 'where', 'who']:
                analysis['sentence_types'].append('question')
            elif any(token.dep_ == 'nsubj' for token in sent_doc):
                analysis['sentence_types'].append('statement')
            else:
                analysis['sentence_types'].append('command')
    
    # Identify the main topic
    analysis['main_topic'] = get_main_topic(doc, analysis['nouns'])
    
    # Identify content type
    analysis['content_type'] = identify_content_type(doc, analysis)
    
    return analysis

def get_main_topic(doc, nouns):
    """Identifies the main topic of the text."""
    # Use noun frequency and position
    noun_freq = Counter(nouns)
    
    # Consider nouns that appear in the first sentence more important
    first_sent_nouns = [token.text for token in list(doc.sents)[0] 
                       if token.pos_ in ['NOUN', 'PROPN']]
    
    # Boost frequency of nouns in the first sentence
    for noun in first_sent_nouns:
        if noun in noun_freq:
            noun_freq[noun] += 2
    
    return noun_freq.most_common(1)[0][0] if noun_freq else ""

def identify_content_type(doc, analysis):
    """Identifies the type of content based on various patterns."""
    text = doc.text.lower()
    
    # Check for various content patterns
    patterns = {
        'definition': any(phrase in text for phrase in ['is defined as', 'refers to', 'means', 'is a type of']),
        'process': any(phrase in text for phrase in ['first', 'then', 'next', 'finally', 'steps']),
        'comparison': any(phrase in text for phrase in ['compared to', 'whereas', 'while', 'but', 'however']),
        'example': any(phrase in text for phrase in ['for example', 'such as', 'like', 'instance']),
        'cause_effect': any(phrase in text for phrase in ['because', 'therefore', 'thus', 'as a result']),
        'historical': any(ent.label_ in ['DATE', 'TIME'] for ent in doc.ents),
        'biographical': any(ent.label_ == 'PERSON' for ent in doc.ents),
        'location_based': any(ent.label_ in ['GPE', 'LOC'] for ent in doc.ents)
    }
    
    # Return the most likely content type
    for content_type, exists in patterns.items():
        if exists:
            return content_type
    return 'general'

def generate_instruction_templates():
    """Returns a dictionary of instruction templates for different content types."""
    return {
        'definition': [
            "Explain what {topic} means and why it's important",
            "Define {topic} and describe its key characteristics",
            "What is {topic} and how does it work?",
            "Provide a detailed explanation of {topic}",
        ],
        'process': [
            "Describe the process of {topic}",
            "What are the steps involved in {topic}?",
            "How does {topic} work? Explain the process",
            "Walk through the stages of {topic}",
        ],
        'comparison': [
            "Compare and contrast the different aspects of {topic}",
            "What are the similarities and differences in {topic}?",
            "How do the various elements of {topic} compare?",
            "Analyze the different perspectives on {topic}",
        ],
        'example': [
            "Provide examples of {topic} and explain their significance",
            "What are some notable instances of {topic}?",
            "Illustrate the concept of {topic} with examples",
            "Give specific examples that demonstrate {topic}",
        ],
        'cause_effect': [
            "What are the causes and effects of {topic}?",
            "Explain how {topic} impacts its surroundings",
            "Describe the relationship between {topic} and its effects",
            "What are the consequences of {topic}?",
        ],
        'historical': [
            "Describe the historical significance of {topic}",
            "What role did {topic} play in history?",
            "Explain the historical context of {topic}",
            "How has {topic} evolved over time?",
        ],
        'biographical': [
            "Who is {topic} and why are they significant?",
            "Describe {topic}'s major contributions and achievements",
            "What makes {topic} notable?",
            "Explain the importance of {topic}'s work",
        ],
        'location_based': [
            "Describe the significance of {topic}",
            "What makes {topic} unique or important?",
            "Explain the characteristics of {topic}",
            "Why is {topic} noteworthy?",
        ],
        'general': [
            "Explain the key aspects of {topic}",
            "What are the important elements of {topic}?",
            "Describe the main features of {topic}",
            "Provide an overview of {topic}",
        ]
    }

def generate_smart_instruction(text, nlp):
    """Generates a specific instruction based on detailed content analysis."""
    # Analyze the content
    analysis = analyze_content(text, nlp)
    
    # Get instruction templates
    templates = generate_instruction_templates()
    
    # Select appropriate template based on content type
    content_type = analysis['content_type']
    if content_type in templates:
        template = random.choice(templates[content_type])
    else:
        template = random.choice(templates['general'])
    
    # Format the template with the main topic
    instruction = template.format(topic=analysis['main_topic'])
    
    return instruction

def convert_text_to_json(input_file_path, output_file_path):
    """Converts text file to JSON with enhanced instruction generation."""
    try:
        nlp = load_nlp()
        
        with open(input_file_path, 'r', encoding='utf-8') as file:
            lines = [line.strip() for line in file.readlines() if line.strip()]
        
        json_entries = []
        for i in range(0, len(lines), 3):
            chunk = lines[i:min(i + 3, len(lines))]
            if chunk:
                chunk_text = '\n'.join(chunk)
                entry = {
                    "instruction": generate_smart_instruction(chunk_text, nlp),
                    "response": chunk_text
                }
                json_entries.append(entry)
        
        with open(output_file_path, 'w', encoding='utf-8') as file:
            json.dump(json_entries, file, indent=2, ensure_ascii=False)
            
        print(f"Successfully converted {input_file_path} to {output_file_path}")
        print(f"Generated {len(json_entries)} instruction-response pairs")
        
    except Exception as e:
        print(f"Error processing {input_file_path}: {str(e)}")

def batch_convert_files(input_dir, output_dir):
    """Converts all text files in a directory to JSON format."""
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    for file_path in Path(input_dir).glob('*.txt'):
        output_path = Path(output_dir) / f"{file_path.stem}.json"
        convert_text_to_json(str(file_path), str(output_path))

if __name__ == "__main__":
    convert_text_to_json("chapter1.txt", "output.json")